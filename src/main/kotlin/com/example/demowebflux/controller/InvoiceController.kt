package com.example.demowebflux.controller

import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("invoice")
class InvoiceController(
    private val invoiceService: InvoiceService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun add(@RequestBody @Valid request: InvoiceRequestDTO): InvoiceResponseDTO {
        return invoiceService.create(request).let {
            InvoiceResponseDTO(
                it
            )
        }
    }
}