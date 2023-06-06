package com.example.demowebflux.controller

import com.example.demowebflux.controller.dto.AvailableCurrenciesResponseDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.controller.dto.MerchantInvoiceResponseDTO
import com.example.demowebflux.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invoice")
class InvoiceController(
    private val invoiceService: InvoiceService,
) {

    /**
     * мерчант хочет создать счет
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun add(@RequestBody @Valid request: InvoiceRequestDTO): MerchantInvoiceResponseDTO {
        return invoiceService.create(request).let(invoiceService::mapToResponse)
    }

    /**
     * виджету надо получить список доступных валют на которые может платить клиент
     */
    @GetMapping("{id}/available")
    suspend fun getAvailableCurrencies(
        @PathVariable id: String
    ): AvailableCurrenciesResponseDTO {
        return invoiceService.getAvailableCurrenciesForInvoice(id)
    }

    @GetMapping("{id}")
    suspend fun get(
        @PathVariable id: String
    ): InvoiceResponseDTO {
        return invoiceService.getByExternalId(id)
    }
}