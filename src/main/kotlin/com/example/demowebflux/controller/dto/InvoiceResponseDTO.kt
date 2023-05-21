package com.example.demowebflux.controller.dto

import com.example.jooq.tables.pojos.Invoice
import java.math.BigDecimal
import java.util.*

data class InvoiceResponseDTO(
    val id: UUID,
    val merchantId: UUID,
    val customerId: String,
    val orderId: String,
    val currency: String,
    val amount: BigDecimal,
    val description: String?,
    val successUrl: String,
    val failUrl: String
) {
    constructor(model: Invoice) : this(
        model.id,
        model.merchantId,
        model.customerId,
        model.merchantOrderId,
        // TODO currency
        model.currencyId.toString(),
        model.amount,
        model.description,
        model.successUrl,
        model.failUrl
    )
}
