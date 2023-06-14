package com.example.demowebflux.repo

import com.example.jooq.Tables.BLOCKCHAIN_TRANSACTION_PENDING
import com.example.jooq.tables.pojos.BlockchainTransactionPending
import com.example.jooq.tables.records.BlockchainTransactionPendingRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

    fun update(entity: BlockchainTransactionPending, context: DSLContext): Mono<BlockchainTransactionPending> {
        return context.update(BLOCKCHAIN_TRANSACTION_PENDING)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.COMPLETED, entity.completed)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.CONFIRMATIONS, entity.confirmations)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.CONFIRMED, entity.confirmed)
            .set(BLOCKCHAIN_TRANSACTION_PENDING.COMPLETED_AD, entity.completedAd)
            .where(BLOCKCHAIN_TRANSACTION_PENDING.ID.eq(entity.id))
            .returning()
            .toMonoAndMap()
    }
}