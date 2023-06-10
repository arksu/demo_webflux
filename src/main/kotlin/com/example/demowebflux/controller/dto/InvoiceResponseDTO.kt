package com.example.demowebflux.controller.dto

import com.example.jooq.enums.OrderStatusType
import java.math.BigDecimal
import java.time.OffsetDateTime

data class InvoiceResponseDTO(
    /**
     * внешний ид счета
     */
    val invoiceId: String,

    /**
     * имя магазина мерчанта
     */
    val shopName: String,

    /**
     * дата закрытия счета
     */
    val deadline: OffsetDateTime?,

    /**
     * статус счета (сделки) (NEW - если еще не выбрали валюту)
     */
    val status: OrderStatusType,

    /**
     * сумма по счету
     */
    val invoiceAmount: BigDecimal,

    /**
     * валюта по счету (которую хочет мерчант)
     */
    val invoiceCurrency: String,

    /**
     * куда надо перевести деньги клиенту
     */
    val walletAddress: String?,

    /**
     * валюта в которой происходит оплата клиентом
     */
    val currency: String?,

    /**
     * полная сумма сделки
     */
    val amount: BigDecimal?,

    /**
     * какую сумму, полученную от клиента уже увидела система
     */
    val amountReceived: BigDecimal?,

    /**
     * сколько осталось перевести клиенту для завершения сделки
     */
    val amountPending: BigDecimal?,
)
