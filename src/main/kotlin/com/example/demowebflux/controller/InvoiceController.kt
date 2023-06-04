package com.example.demowebflux.controller

import com.example.demowebflux.controller.dto.AvailableCurrenciesResponseDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.controller.dto.OrderResponseDTO
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.service.InvoiceService
import com.example.demowebflux.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invoice")
class InvoiceController(
    private val invoiceService: InvoiceService,
    private val currencyService: CurrencyService,
    private val orderService: OrderService,
) {

    /**
     * мерчант хочет создать счет
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun add(@RequestBody @Valid request: InvoiceRequestDTO): InvoiceResponseDTO {
        return invoiceService.create(request).let(invoiceService::mapToResponse)
    }

    /**
     * виджету надо получить список доступных валют на которые может платить клиент
     */
    @GetMapping("{id}/available")
    suspend fun getAvailableCurrencies(
        @PathVariable id: String
    ): AvailableCurrenciesResponseDTO {
        return invoiceService.getAvailableForInvoice(id)
    }

    @GetMapping("{id}/order")
    suspend fun getOrder(
        @PathVariable id: String
    ): OrderResponseDTO {
        return orderService.getByInvoiceExternalId(id)
    }
}