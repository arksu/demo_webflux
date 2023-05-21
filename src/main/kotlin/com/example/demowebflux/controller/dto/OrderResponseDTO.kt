package com.example.demowebflux.controller.dto

import com.example.jooq.tables.pojos.Order
import java.util.*

data class OrderResponseDTO(
    val id: UUID,
    val invoiceId: UUID
) {
    constructor(model: Order) : this(
        id = model.id,
        invoiceId = model.invoiceId
    )
}
