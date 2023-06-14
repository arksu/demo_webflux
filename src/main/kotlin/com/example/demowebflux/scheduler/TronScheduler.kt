package com.example.demowebflux.scheduler

import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.BlockchainTransactionPendingRepo
import com.example.demowebflux.repo.BlockchainTransactionRepo
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.service.TronService
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.enums.BlockchainType
import com.example.jooq.tables.pojos.*
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@DependsOn("currencyService")
class TronScheduler(
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val blockchainTransactionRepo: BlockchainTransactionRepo,
    private val blockchainTransactionPendingRepo: BlockchainTransactionPendingRepo,
    private val currencyService: CurrencyService,
    private val tronService: TronService,
    private val dslContext: DSLContext,
) {
    val log by LoggerDelegate()

    val nileCurrencies: MutableList<Int> = ArrayList()
    val shastaCurrencies: MutableList<Int> = ArrayList()
    val mainCurrencies: MutableList<Int> = ArrayList()

    @PostConstruct
    fun init() {
        mainCurrencies.clear()
        mainCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON).map {
            it.id
        })
        shastaCurrencies.clear()
        shastaCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON_SHASTA).map {
            it.id
        })
        nileCurrencies.clear()
        nileCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON_NILE).map {
            it.id
        })

        val (k, a) = tronService.generateWallet()
        println("$a : $k")
    }

    @Scheduled(fixedDelay = 1000)
    fun updatePendingTransactions() {
        blockchainTransactionPendingRepo.findAllNotCompleted(dslContext)
            .flatMap {
                val info = tronService.getTronscanTransactionInfo(it.id, it.blockchain)
                info
            }
            .doOnNext {
                println(it)
            }
            .blockLast()
    }

    @Scheduled(fixedDelayString = "\${app.trongrid.updateInterval}")
    fun updateNewTransactions() {
        log.debug("begin update tron")

        /*
        https://developers.tron.network/v3.7/reference/trc20-transaction-information-by-account-address

        запоминаем все транзакции аккаунта
        tronService.getUsdtTransactionsByAccount
        ищем нашу новую транзакцию

        получаем транзакцию которая в ожидании
        теребим ее https://nileapi.tronscan.org/api/transaction-info?hash={hash}
        обновляем отсюда "confirmations": 30
        как только возвращает "confirmed": true

            начинаем ходить еще и в tronService.getUsdtConfirmedTransactionsByAccount
            убеждаемся что наша транзакция есть. сумма из тронскана и trongrid совпадает
         */


        // найдем занятые кошельки по трону
        blockchainIncomeWalletRepo
            .findIsBusy(nileCurrencies, dslContext)
            .flatMap { (wallet, order) ->
                mono {
                    val currency = currencyService.getById(order.selectedCurrencyId)
                    if (wallet.currencyId == order.selectedCurrencyId && currency.name.startsWith("USDT")) {
                        processUsdt(wallet, order, currency)
                    }
                }
            }
//            .parallel()
//            .sequential()
            .blockLast()
        log.debug("finish update tron")
    }

    suspend fun processUsdt(wallet: BlockchainIncomeWallet, order: Order, currency: Currency) {
        log.debug("start process ${wallet.address}")
        // получаем транзакции по адресу
        val trxList = tronService.getUsdtTransactionsByAccount(wallet.address, currency.blockchain)
            .collectList().awaitSingleOrNull()
        if (trxList.isNullOrEmpty()) {
            log.debug("stop process ${wallet.address} with empty")
            return
        }

        // получаем ид транзакций
        val ids = trxList.map {
            it.transaction_id
        }
        log.debug("${wallet.address} trxs : ${trxList.size}")
        // получили список транзакций которые у нас есть в базе из тех что получили
        val savedTrxList = blockchainTransactionRepo.findAllInList(ids, dslContext).collectList().awaitSingleOrNull() ?: return
        val savedIds = savedTrxList.map {
            it.id
        }

        // оставляем только те которых у нас нет в базе
        val work = (ids - savedIds.toSet()).toSet()
        log.debug("work ${work.size}")
        // приводим к рабочему виду
        val workTrx = trxList.filter {
            work.contains(it.transaction_id)
        }
        log.debug("workTrx ${workTrx.size}")

        workTrx.forEach { trx ->
            val decimals = trx.token_info?.decimals
            val symbol = trx.token_info?.symbol
            if (decimals != null && "usdt".equals(symbol, ignoreCase = true)) {
                val weiFactor = BigDecimal.TEN.pow(decimals)
                val amount = BigDecimal(trx.value).divide(weiFactor)

                dslContext.transactionCoroutine { trxDsl ->
                    val context = trxDsl.dsl()

                    if (trx.to == wallet.address && amount >= order.customerAmountPending) {
                        // мы нашли транзакцию на нужную нам сумму. теперь надо убедиться что она пройдет по блокчейну и будет подтверждена
                        // сохраним ее в pending
                        println("WIN!")

                        val pendingTrx = BlockchainTransactionPending()
                        pendingTrx.id = trx.transaction_id
                        pendingTrx.confirmations = 0
                        pendingTrx.completed = false
                        pendingTrx.confirmed = false
                        pendingTrx.blockchain = currency.blockchain
                        blockchainTransactionPendingRepo.save(pendingTrx, context).awaitSingle()
                    }

                    val savedTrx = BlockchainTransaction()
                    savedTrx.id = trx.transaction_id
                    savedTrx.amount = amount
                    savedTrx.walletId = wallet.id
                    savedTrx.fromAddress = trx.from
                    savedTrx.toAddress = trx.to
                    savedTrx.blockchain = currency.blockchain
                    savedTrx.currencyId = currency.id
                    blockchainTransactionRepo.save(savedTrx, context).awaitSingle()
                }
            }
        }

        log.debug("stop process ${wallet.address}")
    }
}