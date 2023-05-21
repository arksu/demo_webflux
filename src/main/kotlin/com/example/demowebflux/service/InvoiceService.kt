package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.MerchantRepo
import com.example.jooq.tables.pojos.Invoice
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InvoiceService(
    private val merchantRepo: MerchantRepo,
    private val currencyService: CurrencyService,
    private val invoiceRepo: InvoiceRepo,
) {
    suspend fun create(request: InvoiceRequestDTO): Invoice {
        val merchant =
            merchantRepo.findById(request.merchantId).awaitSingleOrNull() ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant not found")
        val currency = currencyService.getByName(request.currency) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")
        if (!currency.enabled) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")

        val new = Invoice()
        new.merchantId = merchant.id
        new.customerId = request.customerId
        new.merchantOrderId = request.orderId
        new.currencyId = currency.id
        new.amount = request.amount
        new.commission = merchant.commission
        new.description = request.description
        new.successUrl = request.successUrl
        new.failUrl = request.failUrl

        return invoiceRepo.save(new).awaitSingle()
    }
}