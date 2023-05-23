package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.repo.AccountRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@Service
class OrderService(
    private val currencyService: CurrencyService,
    private val invoiceRepo: InvoiceRepo,
    private val orderRepo: OrderRepo,
    private val dslContext: DSLContext,
    private val accountRepo: AccountRepo
) {
    val log by LoggerDelegate()

    suspend fun startOrderTest(request: CreateOrderRequestDTO): Order {
        return dslContext.transactionCoroutine { trx ->
            val invoice = invoiceRepo.findByIdForUpdate(request.invoiceId, trx.dsl()).awaitFirstOrNull()
                ?: throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "Invoice is not found or locked")

            val new = Order()
            new.invoiceId = invoice.id
            new.customerAmount = invoice.amount
            new.customerAmountFact = invoice.amount
            new.merchantAmountOrder = invoice.amount
            new.merchantAmount = invoice.amount
            new.referenceAmount = invoice.amount
            new.exchangeRate = BigDecimal.ONE
            new.selectedCurrencyId = currencyService.getByName(request.selectedCurrency).id
            new.status = OrderStatusType.NEW
            new.confirmations = 0
            new.merchantCommission = BigDecimal.ZERO
            new.systemCommission = BigDecimal.ZERO

            val save = orderRepo.save(new, trx.dsl()).awaitFirst()

            // TODO debug delay
            withContext(Dispatchers.IO) {
                delay(10)
            }
            log.debug("after delay")

            // TODO test
            accountRepo.add(request.invoiceId.toString(), trx.dsl()).awaitFirstOrNull()

            save
        }
    }

    suspend fun startOrder(request: CreateOrderRequestDTO): Order {
        return dslContext.transactionCoroutine { trx ->
            val invoice = invoiceRepo.findByIdForUpdate(request.invoiceId, trx.dsl()).awaitFirstOrNull()
                ?: throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "Invoice is not found or locked")

            val new = Order()
            new.invoiceId = invoice.id
            new.customerAmount = invoice.amount
            new.customerAmountFact = invoice.amount
            new.merchantAmountOrder = invoice.amount
            new.merchantAmount = invoice.amount
            new.referenceAmount = invoice.amount
            new.exchangeRate = BigDecimal.ONE
            new.selectedCurrencyId = currencyService.getByName(request.selectedCurrency).id
            new.status = OrderStatusType.NEW
            new.confirmations = 0
            new.merchantCommission = BigDecimal.ZERO
            new.systemCommission = BigDecimal.ZERO

            val save = orderRepo.save(new, trx.dsl()).awaitFirst()

            save
        }
    }
}