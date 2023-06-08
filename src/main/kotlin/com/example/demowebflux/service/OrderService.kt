package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.converter.InvoiceDTOConverter
import com.example.demowebflux.exception.BadRequestException
import com.example.demowebflux.exception.InvoiceNotFoundOrLockedException
import com.example.demowebflux.exception.NoFreeWalletException
import com.example.demowebflux.exception.UnprocessableEntityException
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderOperationLogRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.pojos.OrderOperationLog
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class OrderService(
    private val dslContext: DSLContext,
    private val currencyService: CurrencyService,
    private val calcService: CalcService,
    private val merchantService: MerchantService,
    private val exchangeRateService: ExchangeRateService,
    private val shopService: ShopService,
    private val invoiceRepo: InvoiceRepo,
    private val orderRepo: OrderRepo,
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val orderOperationLogRepo: OrderOperationLogRepo,
    private val invoiceDTOConverter: InvoiceDTOConverter,

    @Value("\${app.decimalScale}")
    private val scale: Int
) {
    val log by LoggerDelegate()

    /**
     * клиент выбрал валюту, запускаем сделку, выбираем кошелек под эту валюту
     */
    suspend fun startOrder(request: CreateOrderRequestDTO): InvoiceResponseDTO {
        val targetCurrency = currencyService.getByName(request.selectedCurrency)

        return dslContext.transactionCoroutine { trx ->
            val invoice = invoiceRepo.findByExternalIdForUpdateSkipLocked(request.invoiceId, trx.dsl()).awaitSingleOrNull()
                ?: throw InvoiceNotFoundOrLockedException(request.invoiceId)

            if (invoice.status != InvoiceStatusType.NEW) {
                throw UnprocessableEntityException("Wrong invoice status")
            }
            invoice.status = InvoiceStatusType.PROCESSING
            invoiceRepo.updateStatus(invoice, trx.dsl()).awaitFirst()

            val shop = shopService.getById(invoice.shopId, trx.dsl())
            val merchant = merchantService.getById(shop.merchantId, trx.dsl())
            val fromCurrency = currencyService.getById(invoice.currencyId)

            // ищем свободные кошельки на которые можем принять
            val freeWallets = blockchainIncomeWalletRepo
                .findByCurrencyAndFreeForUpdateSkipLocked(targetCurrency.id, trx.dsl())
                .awaitSingleOrNull()
                ?: throw NoFreeWalletException()

            if (freeWallets.isEmpty()) {
                throw NoFreeWalletException()
            }
            // берем первый из перемешанного списка свободных кошельков
            val wallet = freeWallets.shuffled()[0]

            val exchangeRate = exchangeRateService.getRate(fromCurrency, targetCurrency)
                ?: throw BadRequestException("Incorrect selected currency, try other")

            val new = Order()
            new.status = OrderStatusType.PENDING
            new.invoiceId = invoice.id

            // эталонная сумма сделки в валюте которую выбрал клиент, от которой идет расчет (invoice.amount -> exchange_rate[selected_currency_id])
            val referenceAmount = invoice.amount * exchangeRate
            // запишем комиссию мерчанта на момент создания сделки
            new.commission = merchant.commission
            // запишем сумму счета на основании которого создается сделка
            new.invoiceAmount = invoice.amount

            // производим расчет сумм
            val calcModel = calcService.calcOrderAmounts(invoice, shop, referenceAmount, merchant.commission)

            // сколько ожидаем получить от клиента, чтобы считать сделку завершенной
            new.customerAmount = calcModel.customerAmount
            // сколько из этого причитается мерчанту в валюте сделки
            new.merchantAmountByOrder = calcModel.merchantAmountByOrder
            // сколько должны мерчанту в валюте счета (в той в которой он изначально хотел получить)
            new.merchantAmountByInvoice = calcModel.merchantAmountByInvoice

            // сколько фактически пришло от клиента (может он отправил больше чем надо)
            // на момент начала сделки ставим ноль
            new.customerAmountReceived = BigDecimal.ZERO
            // сколько еще осталось получить по сделке
            new.customerAmountPending = calcModel.customerAmount

            // сумма комиссии системы которую взимаем со сделки
            new.commissionAmount = new.customerAmount - new.merchantAmountByOrder
            if (new.commissionAmount < BigDecimal.ZERO) {
                throw UnprocessableEntityException("Commission could not be negative")
            }

            // сохраняем в базу расчеты с учетом установленной в системе погрешности
            new.exchangeRate = exchangeRate.setScale(scale, RoundingMode.HALF_UP)
            new.referenceAmount = referenceAmount.setScale(scale, RoundingMode.HALF_UP)
            new.selectedCurrencyId = targetCurrency.id
            new.confirmations = 0

            // сохраняем ордер в базу
            val saved = orderRepo.save(new, trx.dsl()).awaitSingle()
            // сохраним в историю состояний ордера
            saveLog(OrderStatusType.NEW, saved, trx.dsl())

            // занимаем кошелек
            wallet.orderId = saved.id
            blockchainIncomeWalletRepo.updateOrderId(wallet, trx.dsl()).awaitSingle()

            invoiceDTOConverter.toInvoiceResponseDTO(invoice, saved, wallet)
        }
    }

    suspend fun saveLog(fromStatus: OrderStatusType, order: Order, context: DSLContext) {
        orderOperationLogRepo.save(
            OrderOperationLog().let {
                it.orderId = order.id
                it.fromStatus = fromStatus
                it.toStatus = order.status
                it.customerAmount = order.customerAmount
                it.customerAmountPending = order.customerAmountPending
                it.customerAmountReceived = order.customerAmountReceived
                it
            },
            context
        ).awaitSingle()
    }

}