package com.example.demowebflux.repo

import com.example.jooq.Tables.BLOCKCHAIN_TRANSACTION_PENDING
import com.example.jooq.tables.pojos.BlockchainTransactionPending
import com.example.jooq.tables.records.BlockchainTransactionPendingRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class BlockchainTransactionPendingRepo : AbstractCrudRepo<String, BlockchainTransactionPending, BlockchainTransactionPendingRecord>() {
    override val table = BLOCKCHAIN_TRANSACTION_PENDING
    override val idField = BLOCKCHAIN_TRANSACTION_PENDING.ID
    override val mapper = { it: BlockchainTransactionPendingRecord -> BlockchainTransactionPending(it) }

    fun findAllNotCompleted(context: DSLContext): Flux<BlockchainTransactionPending> {
        return context.selectFrom(BLOCKCHAIN_TRANSACTION_PENDING)
            .where(BLOCKCHAIN_TRANSACTION_PENDING.COMPLETED.isFalse)
            .toFluxAndMap()
    }

    fun findCountNotCompletedByOrderId(orderId: UUID, context: DSLContext): Mono<Int> {
        return context.selectCount()
            .from(BLOCKCHAIN_TRANSACTION_PENDING)
            .where(BLOCKCHAIN_TRANSACTION_PENDING.COMPLETED.isFalse)
            .and(BLOCKCHAIN_TRANSACTION_PENDING.ORDER_ID.eq(orderId))
            .toMono()
            .map {
                it.value1()
            }
    }

    fun update(entity: BlockchainTransactionPending, context: DSLContext): Mono<BlockchainTransactionPending> {
        return context.update(BLOCKCHAIN_TRANSACTION_PENDING)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.COMPLETED, entity.completed)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.CONFIRMATIONS, entity.confirmations)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.CONFIRMED, entity.confirmed)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.COMPLETED_AT, entity.completedAt)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.UPDATED_AT, entity.updatedAt)
            .where(BLOCKCHAIN_TRANSACTION_PENDING.ID.eq(entity.id))
            .returning()
            .toMonoAndMap()
    }
}