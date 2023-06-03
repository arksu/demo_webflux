package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.AvailableCurrenciesResponseDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.exception.BadRequestException
import com.example.demowebflux.exception.InvoiceNotFoundOrLockedException
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
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class InvoiceService(
    private val dslContext: DSLContext,
    private val merchantService: MerchantService,
    private val currencyService: CurrencyService,
    private val exchangeRateService: ExchangeRateService,
    private val invoiceRepo: InvoiceRepo,
    @Value("\${app.decimalScale:8}")
    private val decimalScale: Int,
    @Value("\${app.widgetUrl}")
    private val widgetUrl: String
) {
    suspend fun create(request: InvoiceRequestDTO): Invoice {
        val merchant = merchantService.getById(request.merchantId, dslContext)
        if (merchant.apiKey != request.apiKey) {
            throw BadRequestException("Wrong api key")
        }
        val currency = currencyService.getByName(request.currency)

        val new = Invoice()
        new.status = InvoiceStatusType.NEW
        new.merchantId = merchant.id
        new.customerId = request.customerId
        new.merchantOrderId = request.orderId
        new.currencyId = currency.id
        new.amount = request.amount.setScale(decimalScale, RoundingMode.FLOOR)
        new.description = request.description
        new.successUrl = request.successUrl
        new.failUrl = request.failUrl
        new.commissionType = request.commissionCharge
        new.apiKey = merchant.apiKey
        new.externalId = randomStringByKotlinRandom(32)

        try {
            return invoiceRepo.save(new, dslContext).awaitSingle()
        } catch (e: IntegrityConstraintViolationException) {
            throw BadRequestException("Check your request for uniq (orderId)")
        }
    }

    /**
     * получить список валют для конкретного счета
     */
    suspend fun getAvailableForInvoice(id: String): AvailableCurrenciesResponseDTO {
        // ищем счет
        val invoice = invoiceRepo.findByExternalId(id, dslContext).awaitSingleOrNull()
            ?: throw InvoiceNotFoundOrLockedException(id)
        val invoiceCurrency = currencyService.getById(invoice.currencyId)
        // ищем нашего мерчанта
        val merchant = merchantService.getById(invoice.merchantId, dslContext)
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
                val rate = exchangeRateService.getRate(it, invoiceCurrency)
                it to rate
            }
            // исключим те валюты по которым нет курса
            .filter { (_, rate) -> rate != null }
            .map { (curr, rate) ->

                AvailableCurrenciesResponseDTO.CurrencyAndRate(
                    name = curr.name,
                    rate = rate!!,
                    // TODO комиссия и тд. полный расчет суммы
                    amount = invoice.amount * BigDecimal.ONE.divide(rate, decimalScale, RoundingMode.HALF_UP)
                )
            }
        return AvailableCurrenciesResponseDTO(list)
    }


    fun mapToResponse(invoice: Invoice): InvoiceResponseDTO {
        return InvoiceResponseDTO(
            id = invoice.externalId,
            // TODO currency
            currency = currencyService.getById(invoice.currencyId).name,
            amount = invoice.amount,
            successUrl = invoice.successUrl,
            failUrl = invoice.failUrl,
            paymentUrl = "$widgetUrl?id=${invoice.externalId}"
        )
    }
}