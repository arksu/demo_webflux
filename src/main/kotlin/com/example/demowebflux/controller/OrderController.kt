package com.example.demowebflux.controller

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/order")
class OrderController(
    private val orderService: OrderService
) {
    /**
     * запускаем создание заказа (клиент выбрал валюту в которой будет платить)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun start(@RequestBody @Valid request: CreateOrderRequestDTO): InvoiceResponseDTO {
        return orderService.startOrder(request)
    }
}