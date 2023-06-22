package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.converter.InvoiceDTOConverter
import com.example.demowebflux.exception.*
import com.example.demowebflux.model.AddressModeEnum
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderOperationLogRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.service.dto.webhook.OrderWebhook
import com.example.demowebflux.util.LoggerDelegate
import com.example.demowebflux.util.percentToMult
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.pojos.OrderOperationLog
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
class OrderService(
    private val dslContext: DSLContext,
    private val currencyService: CurrencyService,
    private val calcService: CalcService,
    private val merchantService: MerchantService,
    private val exchangeRateService: ExchangeRateService,
    private val shopService: ShopService,
    private val walletService: WalletService,
    private val webhookService: WebhookService,
    @Lazy
    private val invoiceService: InvoiceService,
    private val invoiceRepo: InvoiceRepo,
    private val orderRepo: OrderRepo,
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val orderOperationLogRepo: OrderOperationLogRepo,
    private val invoiceDTOConverter: InvoiceDTOConverter,

    @Value("\${app.decimalScale}")
    private val scale: Int,

    @Value("\${app.walletsMode}")
    private val addressMode: AddressModeEnum
) {
    val log by LoggerDelegate()

    val statusToExpire = listOf(OrderStatusType.PENDING, OrderStatusType.NOT_ENOUGH)

    /**
     * клиент выбрал валюту, запускаем сделку, выбираем кошелек под эту валюту
     */
    suspend fun startOrder(request: CreateOrderRequestDTO): InvoiceResponseDTO {
        val targetCurrency = currencyService.getByName(request.selectedCurrency)

        return dslContext.transactionCoroutine { trx ->
            val invoice = invoiceRepo.findByExternalIdForUpdateSkipLocked(request.invoiceId, trx.dsl()).awaitSingleOrNull()
                ?: throw InvoiceNotFoundOrLockedException(request.invoiceId)

            if (invoice.status == InvoiceStatusType.PROCESSING) {
                throw UnprocessableEntityException("This invoice has already started an order")
            }
            if (invoice.status != InvoiceStatusType.NEW) {
                throw UnprocessableEntityException("Wrong invoice status")
            }
            invoice.status = InvoiceStatusType.PROCESSING
            invoiceRepo.updateStatus(invoice, trx.dsl()).awaitFirst()

            val shop = shopService.getById(invoice.shopId, trx.dsl())
            val merchant = merchantService.getById(shop.merchantId, trx.dsl())
            val fromCurrency = currencyService.getById(invoice.currencyId)

            // запрашиваем свободный кошелек
            val wallet = walletService.getFreeWallet(targetCurrency, trx.dsl())

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

    suspend fun expire(order: Order, invoice: Invoice, context: DSLContext) {
        if (statusToExpire.contains(order.status)) {
            // ищем кошелек на который ждали оплаты
            val wallet = blockchainIncomeWalletRepo.findByOrderIdForUpdateSkipLocked(order.id, context).awaitSingleOrNull()
                ?: throw InternalErrorException("Expire order: wallet is not found or locked")

            // только если этот кошелек явно не генерировали под заказ
            if (!wallet.isGenerated) {
                // зануляем заказ на кошельке - чтобы он вернулся в пул
                wallet.orderId = null
                blockchainIncomeWalletRepo.updateOrderId(wallet, context).awaitSingle()
            }

            setOrderStatus(order, invoice, OrderStatusType.EXPIRED, context)
        } else {
            throw InternalErrorException("Expire order: wrong status ${order.status}")
        }
    }

    suspend fun setOrderStatus(order: Order, invoice: Invoice, toStatus: OrderStatusType, context: DSLContext) {
        if (order.status != toStatus) {
            val fromStatus = order.status

            // обновляем статус заказа
            order.status = toStatus
            orderRepo.updateStatus(order, context).awaitSingle()

            webhookService.send(order, invoice, context)

            saveLog(fromStatus, order, context)
        }
    }

    /**
     * добавить в заказ средства от клиента (получили тем или иным способом)
     * двигаем статус заказа
     */
    suspend fun addCustomerAmount(order: Order, amount: BigDecimal, context: DSLContext) {
        log.warn("order add amount=$amount id=${order.id} ")

        val invoice = invoiceRepo.findByIdForUpdateSkipLocked(order.invoiceId, context).awaitSingleOrNull()
            ?: throw InvoiceNotFoundOrLockedException(order.invoiceId.toString())
        val shop = shopService.getById(invoice.shopId, context)

        // обработка поступившей на заказ суммы
        order.customerAmountReceived += amount
        // считаем сколько еще осталось оплатить
        order.customerAmountPending = order.customerAmount - order.customerAmountReceived
        val overpaid = if (order.customerAmountPending < BigDecimal.ZERO) -order.customerAmountPending else BigDecimal.ZERO

        if (order.customerAmountPending < BigDecimal.ZERO) {
            order.customerAmountPending = BigDecimal.ZERO
        }

        // сколько допускается не доплатить по сумме заказа
        val allowed = order.customerAmount.multiply(BigDecimal.ONE - shop.underpaymentAllowed.percentToMult(scale))
        // если оставшаяся к оплате сумма меньше порога - считаем заказ полностью оплаченным
        if (order.customerAmountPending <= allowed) {
            when (order.status) {
                // если ждали или еще не все оплатили - считаем заказ завершенным
                OrderStatusType.PENDING, OrderStatusType.NOT_ENOUGH -> {
                    // если переплата слишком большая - считаем заказ переплаченным
                    setOrderStatus(
                        order,
                        invoice,
                        if (overpaid > BigDecimal.ZERO)
                            OrderStatusType.OVERPAID
                        else
                            OrderStatusType.COMPLETED,
                        context
                    )
                    // счет на оплату также двигаем в выполненный
                    invoiceService.moveToStatus(invoice, InvoiceStatusType.TERMINATED, context)
                }

                else -> {}
            }
        } else {
            // сумма еще не достаточна для завершения заказа
            when (order.status) {
                OrderStatusType.PENDING, OrderStatusType.NOT_ENOUGH -> setOrderStatus(order, invoice, OrderStatusType.NOT_ENOUGH, context)
                else -> {}
            }
        }


        orderRepo.updateCustomerAmount(order, context).awaitSingle()
    }

}