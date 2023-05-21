package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.Order
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class OrderService(
    private val currencyService: CurrencyService,
    private val invoiceRepo: InvoiceRepo,
    private val orderRepo: OrderRepo
) {
    suspend fun startOrder(request: CreateOrderRequestDTO): Order {
        val invoice = invoiceRepo.findByIdForUpdate(request.invoiceId).awaitSingleOrNull()
            ?: throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "Invoice is not found or locked")

        val new = Order()
        new.invoiceId = invoice.id
        new.amount = request.amount
        new.selectedCurrencyId = currencyService.getByName(request.selectedCurrency).id
        new.status = OrderStatusType.NEW
        new.confirmations = 0
        return orderRepo.save(new).awaitSingle()
    }
}