package com.example.demowebflux.controller.dto

import java.math.BigDecimal

data class OrderResponseDTO(
    /**
     * внешний ид счета
     */
    val invoiceId: String,

    /**
     * куда надо перевести деньги клиенту
     */
    val walletAddress: String,

    /**
     * валюта в которой происходит оплата клиентом
     */
    val currency: String,

    /**
     * полная сумма сделки
     */
    val amount: BigDecimal,

    /**
     * какую сумму полученную от клиента уже увидела система
     */
    val amountReceived: BigDecimal,

    /**
     * сколько осталось перевести клиенту для завершения сделки
     */
    val amountPending: BigDecimal,
)
