package com.example.demowebflux.scheduler

import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.BlockchainTransactionPendingRepo
import com.example.demowebflux.repo.BlockchainTransactionRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.service.OrderService
import com.example.demowebflux.service.TronService
import com.example.demowebflux.service.dto.tronscan.TronscanTransactionInfo
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.enums.BlockchainType
import com.example.jooq.tables.pojos.*
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
@DependsOn("currencyService")
class TronScheduler(
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val blockchainTransactionRepo: BlockchainTransactionRepo,
    private val blockchainTransactionPendingRepo: BlockchainTransactionPendingRepo,
    private val currencyService: CurrencyService,
    private val tronService: TronService,
    private val orderService: OrderService,
    private val orderRepo: OrderRepo,
    private val dslContext: DSLContext,
) {
    val log by LoggerDelegate()

    val tronCurrencies: MutableList<Int> = ArrayList()

    @PostConstruct
    fun init() {
        tronCurrencies.clear()
        tronCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON).map {
            it.id
        })
        tronCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON_SHASTA).map {
            it.id
        })
        tronCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON_NILE).map {
            it.id
        })

        val (k, a) = tronService.generateWallet()
        println("$a : $k")
    }

    /**
     * обновляем транзакции ожидающие подтверждения (это явное новые транзакции по которым не было инфы)
     * они должны изменить статус определенного заказа
     */
    @Scheduled(fixedDelay = 2000)
    fun updatePendingTransactions() {
        blockchainTransactionPendingRepo.findAllNotCompleted(dslContext)
            .flatMap { trxPending ->
                val info = tronService.getTronscanTransactionInfo(trxPending.id, trxPending.blockchain).map {
                    Pair(it, trxPending)
                }
                info
            }
            .flatMap { (info, trxPending) ->
                mono { processPendingTransaction(info, trxPending) }
            }
            .blockLast()
    }

    @Scheduled(fixedDelayString = "\${app.trongrid.updateInterval}")
    fun updateNewTransactions() {
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
            .findIsBusy(tronCurrencies, dslContext)
            .flatMap { (wallet, order) ->
                mono {
                    val currency = currencyService.getById(order.selectedCurrencyId)
                    if (wallet.currencyId == order.selectedCurrencyId && currency.contractAddress != null) {
                        processToken(wallet, order, currency)
                    }
                }
            }
            .blockLast()
    }

    /**
     * обработать перевод Token в троне
     */
    suspend fun processToken(wallet: BlockchainIncomeWallet, order: Order, currency: Currency) {
        // получаем крайние транзакции по адресу
        val trxList = tronService.getTokenTransactionsByAccount(wallet.address, currency)
            .collectList()
            .awaitSingleOrNull()
            ?.filter {
                // оставляем только входящие транзакции НА адрес кошелька
                wallet.address.equals(it.to, ignoreCase = true)
            }
        // если нет транзакций по адресу - сразу выходим
        if (trxList.isNullOrEmpty()) {
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

        // оставляем только те которых у нас нет в базе, но есть в блокчейне
        val work = (ids - savedIds.toSet()).toSet()
        log.debug("work ${work.size}")

        // в работу берем только те которых явно у нас нет в базе. только новые транзакции
        // в виде транзакций из блокчейна
        val workTrx = trxList.filter {
            work.contains(it.transaction_id)
        }
        log.debug("workTrx ${workTrx.size}")

        workTrx.forEach { trx ->
            val decimals = trx.token_info?.decimals
            val symbol = trx.token_info?.symbol

            // еще раз убедимся что это точно Token
            if (decimals != null && currency.token.equals(symbol, ignoreCase = true)) {
                val weiFactor = BigDecimal.TEN.pow(decimals)
                // корректно считаем сумму транзакции
                val amount = BigDecimal(trx.value).divide(weiFactor)

                dslContext.transactionCoroutine { trxDsl ->
                    val context = trxDsl.dsl()

                    if (trx.to == wallet.address) {
                        // мы нашли транзакцию. теперь надо убедиться что она пройдет по блокчейну и будет подтверждена
                        // сохраним ее в pending - особая очередь транзакций за которым досканально следим и именно они двигают статус заказов

                        val pendingTrx = BlockchainTransactionPending()
                        pendingTrx.id = trx.transaction_id
                        pendingTrx.confirmations = 0
                        pendingTrx.completed = false
                        pendingTrx.confirmed = false
                        pendingTrx.blockchain = currency.blockchain
                        pendingTrx.orderId = order.id
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
    }

    suspend fun processPendingTransaction(info: TronscanTransactionInfo, trxPending: BlockchainTransactionPending) {
        dslContext.transactionCoroutine { trx ->
            val context = trx.dsl()

            // блокируем транзакцию по которой работаем
            val lockedPendingTrx = blockchainTransactionPendingRepo.findByIdForUpdateSkipLocked(trxPending.id, context).awaitSingleOrNull()
                ?: return@transactionCoroutine

            val order = orderRepo.findByIdForUpdateSkipLocked(lockedPendingTrx.orderId, context).awaitSingleOrNull()
            if (order == null) {
                log.warn("order is not found or locked ${lockedPendingTrx.orderId}")
                return@transactionCoroutine
            }
            val wallet = blockchainIncomeWalletRepo.findByOrderIdForUpdateSkipLocked(order.id, context).awaitSingleOrNull()
            if (wallet == null) {
                log.warn("wallet is not found or locked for order id: ${lockedPendingTrx.orderId}")
                return@transactionCoroutine
            }
            if (wallet.currencyId != order.selectedCurrencyId) {
                throw IllegalStateException("order and wallet currencies does not match")
            }
            val currency = currencyService.getById(order.selectedCurrencyId)

            // обработка токенов
            if (currency.contractAddress != null && info.contract_type == "trc20") {

                // люто валидируем все
                val contractInfo = info.contractInfo?.get(currency.contractAddress)
                if (contractInfo == null) throw IllegalStateException("no contractInfo in tronscan trx")

                if (!contractInfo.isToken) throw IllegalStateException("this is not token")
                if (!currency.token.equals(contractInfo.name, ignoreCase = true)) throw IllegalStateException("wrong token name")

                if (info.trc20TransferInfo.isNullOrEmpty() || info.tokenTransferInfo == null) throw IllegalStateException("no trc20 info")
                if (info.transfersAllList.isNullOrEmpty()) throw IllegalStateException("no transfers list")

                val transferInfo = info.trc20TransferInfo[0]
                if (info.tokenTransferInfo.amount_str != transferInfo.amount_str) throw IllegalStateException("wrong amount_str")
                if (info.tokenTransferInfo.decimals != transferInfo.decimals) throw IllegalStateException("wrong decimals")
                if (info.tokenTransferInfo.symbol != transferInfo.symbol || !currency.token.equals(transferInfo.symbol, ignoreCase = true)) throw IllegalStateException("wrong symbol")

                if (info.tokenTransferInfo.to_address != wallet.address) throw IllegalStateException("wrong receiver address")
                if (transferInfo.to_address != wallet.address) throw IllegalStateException("wrong receiver address")

                val decimals = BigDecimal.TEN.pow(info.tokenTransferInfo.decimals)
                val amount = BigDecimal(info.tokenTransferInfo.amount_str).divide(decimals)
                log.warn("tronscan trx [${wallet.address}] amount $amount ${info.tokenTransferInfo.symbol} confirmed: ${info.confirmed} [${info.confirmations}]")

                lockedPendingTrx.confirmations = info.confirmations
                lockedPendingTrx.confirmed = info.confirmed

                // если пришло подтверждений больше чем требуется
                if (info.confirmed && info.confirmations >= currency.confirmationsRequired) {
                    // теперь сходим еще в trongrid. запросим только подтвержденные транзакции
                    // убедимся что он ее тоже видит как подтвержденную
                    val trxConfirmed = tronService.getTokenConfirmedTransactionsByAccount(wallet.address, currency).collectList().awaitSingleOrNull()
                    log.warn("trongrid confirmed trx: ${trxConfirmed?.size}")
                    if (trxConfirmed.isNullOrEmpty()) throw IllegalStateException("no confirmed transactions from trongrid")

                    val confirmed = trxConfirmed.filter {
                        it.transaction_id == info.hash
                            && it.value == info.tokenTransferInfo.amount_str
                            && it.token_info?.decimals == info.tokenTransferInfo.decimals
                            && "transfer".equals(it.type, ignoreCase = true)
                            && it.to == wallet.address
                            && currency.token.equals(it.token_info.symbol, ignoreCase = true)
                    }
                    // у нас по всем условиям есть строго! одна! подтвержденная транзакция
                    if (confirmed.isNotEmpty() && confirmed.size == 1) {
                        // по транзакции пришли деньги. она по любому помечается завершенной. по ней больше не надо ничего считать.
                        // считаем ее полностью обработанной и никогда к ней больше не вернемся
                        orderService.addCustomerAmount(order, amount, context)
                        lockedPendingTrx.completed = true
                        lockedPendingTrx.completedAt = OffsetDateTime.now()
                    }
                }

                // даже если транзакция еще подтверждена - запомним факт обработки
                lockedPendingTrx.updatedAt = OffsetDateTime.now()
                blockchainTransactionPendingRepo.update(lockedPendingTrx, context).awaitSingle()
            }

            // TODO обработка обычных TRX
        }
    }
}