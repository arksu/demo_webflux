package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.repo.InvoiceRepo
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.tables.pojos.Invoice
import kotlinx.coroutines.reactor.awaitSingle
import org.jooq.DSLContext
import org.jooq.exception.IntegrityConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.RoundingMode

@Service
class InvoiceService(
    private val dslContext: DSLContext,
    private val merchantService: MerchantService,
    private val currencyService: CurrencyService,
    private val invoiceRepo: InvoiceRepo,
    @Value("\${app.decimalScale:8}")
    private val decimalScale: Int
) {
    suspend fun create(request: InvoiceRequestDTO): Invoice {
        val merchant = merchantService.getById(request.merchantId)
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

        try {
            return invoiceRepo.save(new, dslContext).awaitSingle()
        } catch (e: IntegrityConstraintViolationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Check your request for uniq (orderId)")
        }
    }
}