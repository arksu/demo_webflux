package com.example.demowebflux.service

import com.example.demowebflux.exception.BadRequestException
import com.example.demowebflux.exception.NoFreeWalletException
import com.example.demowebflux.model.AddressModeEnum
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.jooq.enums.BlockchainType
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.pojos.Currency
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class WalletService(
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val tronService: TronService,

    @Value("\${app.walletsMode}")
    private val mode: AddressModeEnum
) {
    suspend fun getFreeWallet(currency: Currency, context: DSLContext): BlockchainIncomeWallet {
        return when (mode) {
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
                freeWallets.shuffled()[0]
            }

            AddressModeEnum.GENERATE -> {
                when (currency.blockchain) {
                    BlockchainType.TRON, BlockchainType.TRON_SHASTA, BlockchainType.TRON_NILE -> {
                        val (key, address) = tronService.generateWallet()

                        val newWallet = BlockchainIncomeWallet()
                        newWallet.currencyId = currency.id
                        newWallet.address = address
                        newWallet.key = key
                        newWallet.enabled = true

                        blockchainIncomeWalletRepo.save(newWallet, context).awaitSingle()
                    }

                    else -> throw BadRequestException("Blockchain ${currency.blockchain} is not supported")
                }
            }
        }
    }
}