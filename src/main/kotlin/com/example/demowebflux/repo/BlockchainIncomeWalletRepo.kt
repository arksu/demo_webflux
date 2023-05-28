package com.example.demowebflux.repo

import com.example.demowebflux.util.toFlux
import com.example.jooq.Tables.BLOCKCHAIN_INCOME_WALLET
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class BlockchainIncomeWalletRepo {
    fun findByCurrencyAndFree(currencyId: Int, dslContext: DSLContext): Mono<List<BlockchainIncomeWallet>> {
        return dslContext.selectFrom(BLOCKCHAIN_INCOME_WALLET)
            .where(BLOCKCHAIN_INCOME_WALLET.CURRENCY_ID.eq(currencyId))
            .and(BLOCKCHAIN_INCOME_WALLET.IS_BUSY.isFalse)
            .limit(100)
            .forUpdate()
            .skipLocked()
            .toFlux()
            .map(::BlockchainIncomeWallet)
            .collectList()
    }
}