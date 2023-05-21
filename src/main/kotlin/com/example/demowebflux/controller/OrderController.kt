package com.example.demowebflux.controller

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.OrderResponseDTO
import com.example.demowebflux.service.OrderService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("order")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    suspend fun start(@RequestBody @Valid request: CreateOrderRequestDTO): OrderResponseDTO {
        return orderService.startOrder(request).let {
            OrderResponseDTO(it)
        }
    }
}