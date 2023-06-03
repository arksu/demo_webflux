package com.example.demowebflux.repo

import com.example.demowebflux.util.toFlux
import com.example.jooq.Tables.BLOCKCHAIN_INCOME_WALLET
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.records.BlockchainIncomeWalletRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Repository
class BlockchainIncomeWalletRepo : AbstractCrudRepo<UUID, BlockchainIncomeWallet, BlockchainIncomeWalletRecord>() {
    override val table = BLOCKCHAIN_INCOME_WALLET
    override val idField = BLOCKCHAIN_INCOME_WALLET.ID
    override val mapper = { it: BlockchainIncomeWalletRecord -> BlockchainIncomeWallet(it) }

    fun findByCurrencyAndFreeForUpdateSkipLocked(currencyId: Int, context: DSLContext): Mono<List<BlockchainIncomeWallet>> {
        return context.selectFrom(BLOCKCHAIN_INCOME_WALLET)
            .where(BLOCKCHAIN_INCOME_WALLET.CURRENCY_ID.eq(currencyId))
            .and(BLOCKCHAIN_INCOME_WALLET.ORDER_ID.isNull)
            .limit(100)
            .forUpdate()
            .skipLocked()
            .toFlux()
            .map(mapper)
            .collectList()
    }

    /**
     * ищем кошельки занятые работой, по указанному списку валют
     */
    fun findIsBusy(currencies: List<Int>, context: DSLContext): Flux<BlockchainIncomeWallet> {
        return context.selectFrom(BLOCKCHAIN_INCOME_WALLET)
            .where(BLOCKCHAIN_INCOME_WALLET.ORDER_ID.isNotNull)
            .toFlux()
            .map(mapper)
    }

    fun updateOrderId(entity: BlockchainIncomeWallet, context: DSLContext): Mono<BlockchainIncomeWallet> {
        return context.update(BLOCKCHAIN_INCOME_WALLET)
            .set(BLOCKCHAIN_INCOME_WALLET.ORDER_ID, entity.orderId)
            .where(BLOCKCHAIN_INCOME_WALLET.ID.eq(entity.id))
            .returning()
            .toMonoAndMap()

    }
}