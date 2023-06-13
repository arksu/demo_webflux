package com.example.demowebflux.repo

import com.example.jooq.Tables.BLOCKCHAIN_TRANSACTION
import com.example.jooq.tables.pojos.BlockchainTransaction
import com.example.jooq.tables.records.BlockchainTransactionRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class BlockchainTransactionRepo : AbstractCrudRepo<String, BlockchainTransaction, BlockchainTransactionRecord>() {
    override val table = BLOCKCHAIN_TRANSACTION
    override val idField = BLOCKCHAIN_TRANSACTION.ID
    override val mapper = { it: BlockchainTransactionRecord -> BlockchainTransaction(it) }

    fun findAllInList(list: List<String>, context: DSLContext): Flux<BlockchainTransaction> {
        return context.selectFrom(BLOCKCHAIN_TRANSACTION)
            .where(BLOCKCHAIN_TRANSACTION.ID.`in`(list))
            .toFluxAndMap()
    }
}