package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.AvailableCurrenciesResponseDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.controller.dto.MerchantInvoiceResponseDTO
import com.example.demowebflux.converter.InvoiceDTOConverter
import com.example.demowebflux.exception.InvoiceAlreadyExists
import com.example.demowebflux.exception.InvoiceNotFoundException
import com.example.demowebflux.exception.InvoiceNotFoundOrLockedException
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.util.randomStringByKotlinRandom
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.tables.pojos.Invoice
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.jooq.exception.IntegrityConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.OffsetDateTime

@Service
class InvoiceService(
    private val dslContext: DSLContext,
    private val merchantService: MerchantService,
    private val currencyService: CurrencyService,
    private val calcService: CalcService,
    private val exchangeRateService: ExchangeRateService,
    private val invoiceRepo: InvoiceRepo,
    private val shopService: ShopService,
    private val orderRepo: OrderRepo,
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val invoiceDTOConverter: InvoiceDTOConverter,

    @Value("\${app.decimalScale:8}")
    private val decimalScale: Int,
    @Value("\${app.widgetUrl}")
    private val widgetUrl: String
) {
    suspend fun create(request: InvoiceRequestDTO): Invoice {
        // ищем магазин по указанному ключу
        val shop = shopService.getByApiKey(request.apiKey, dslContext)
        val currency = currencyService.getByName(request.currency)

        val new = Invoice()
        new.status = InvoiceStatusType.NEW
        new.shopId = shop.id
        new.customerId = request.customerId
        new.merchantOrderId = request.orderId
        new.currencyId = currency.id
        new.amount = request.amount.setScale(decimalScale, RoundingMode.HALF_UP)
        new.description = request.description
        new.successUrl = request.successUrl
        new.failUrl = request.failUrl
        new.apiKey = request.apiKey
        new.externalId = randomStringByKotlinRandom(32)
        new.deadline = OffsetDateTime.now().plusMinutes(shop.expireMinutes.toLong())

        try {
            return invoiceRepo.save(new, dslContext).awaitSingle()
        } catch (e: IntegrityConstraintViolationException) {
            throw InvoiceAlreadyExists(request.orderId)
        }
    }

    /**
     * получить список валют для конкретного счета
     */
    suspend fun getAvailableCurrenciesForInvoice(id: String): AvailableCurrenciesResponseDTO {
        // ищем счет
        val invoice = invoiceRepo.findByExternalId(id, dslContext).awaitSingleOrNull()
            ?: throw InvoiceNotFoundOrLockedException(id)
        val invoiceCurrency = currencyService.getById(invoice.currencyId)
        val shop = shopService.getById(invoice.shopId, dslContext)
        // ищем нашего мерчанта
        val merchant = merchantService.getById(shop.merchantId, dslContext)
        // получаем список валют доступных для него
        val available = merchantService.getPaymentAvailableCurrencies(merchant)

        // берем все валюты вообще
        val list = currencyService
            .allList
            .filter {
                // фильтруем только те что доступны для мерчанта
                // либо отключаем фильтр если у мерча нет списка
                available == null || available.contains(it.id)
            }
            .map {
                val rate = exchangeRateService.getRate(
                    from = invoiceCurrency,
                    to = it,
                )
                it to rate
            }
            // исключим те валюты по которым нет курса
            .filter { (_, rate) -> rate != null }
            .map { (curr, rate) ->
                val referenceAmount = invoice.amount * rate!!

                val calc = calcService.calcOrderAmounts(invoice, shop, referenceAmount, merchant.commission)
                AvailableCurrenciesResponseDTO.CurrencyAndRate(
                    name = curr.name,
                    rate = rate,
                    // сумма которую должен заплатить клиент
                    amount = calc.customerAmount
                )
            }
        return AvailableCurrenciesResponseDTO(list)
    }

    suspend fun getByExternalId(invoiceExternalId: String): InvoiceResponseDTO {
        val invoice = invoiceRepo.findByExternalId(invoiceExternalId, dslContext).awaitSingleOrNull()
            ?: throw InvoiceNotFoundException(invoiceExternalId)

        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        return when (invoice.status) {
            InvoiceStatusType.NEW -> {
                invoiceDTOConverter.toInvoiceResponseDTO(invoice, null, null)
            }

            InvoiceStatusType.PROCESSING, InvoiceStatusType.TERMINATED -> {
                val order = orderRepo.findByInvoiceId(invoice.id, dslContext).awaitSingle()
                val wallet = blockchainIncomeWalletRepo.findByOrderId(order.id, dslContext).awaitSingle()

                invoiceDTOConverter.toInvoiceResponseDTO(invoice, order, wallet)
            }
        }
    }

    fun mapToResponse(invoice: Invoice): MerchantInvoiceResponseDTO {
        return MerchantInvoiceResponseDTO(
            id = invoice.externalId,
            currency = currencyService.getById(invoice.currencyId).name,
            amount = invoice.amount,
            paymentUrl = "$widgetUrl${invoice.externalId}"
        )
    }
}