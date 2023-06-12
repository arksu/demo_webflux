package com.example.demowebflux.service

import com.example.demowebflux.exception.NoFreeWalletException
import com.example.demowebflux.model.AddressModeEnum
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.pojos.Currency
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class WalletService(
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,

    @Value("\${app.walletsMode}")
    private val mode: AddressModeEnum
) {
    suspend fun getFreeWallet(currency: Currency, context: DSLContext): BlockchainIncomeWallet {
        when (mode) {
            AddressModeEnum.POOL -> {
                // ищем свободные кошельки на которые можем принять
                val freeWallets = blockchainIncomeWalletRepo
                    .findByCurrencyAndFreeForUpdateSkipLocked(currency.id, context)
                    .awaitSingleOrNull()
                    ?: throw NoFreeWalletException()

                if (freeWallets.isEmpty()) {
                    throw NoFreeWalletException()
                }

                // берем первый из перемешанного списка свободных кошельков
                return freeWallets.shuffled()[0]
            }

            AddressModeEnum.GENERATE -> {
                TODO()
            }
        }
    }
}