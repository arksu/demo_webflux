package com.example.demowebflux.service.dto.webhook

import com.example.jooq.enums.OrderStatusType
import java.math.BigDecimal
import java.util.*

data class OrderWebhook(
    val merchantId: UUID,
    /**
     * ид одного из магазинов мерчанта
     */
    val shopId: UUID,

    val shopName: String,

    /**
     * внешний ид счета на оплату
     */
    val invoiceId: String,
    val status: OrderStatusType,

    /**
     * валюта по счету на оплату
     */
    val currency: String,
    /**
     * сумма по счету на оплату
     */
    val amount: BigDecimal,

    /**
     * курс обмена по которому создан заказ для выбранной валюты
     */
    val exchangeRate: BigDecimal,

    /**
     * валюта по заказу
     */
    val orderCurrency: String,
    /**
     * сумма к оплате по заказу в выбранной юзером валюте
     */
    val orderAmount: BigDecimal,
    /**
     * сколько ожидается еще к оплате от клиента
     */
    val orderPendingAmount: BigDecimal,
    /**
     * сколько уже оплатил клиент по заказу
     */
    val orderReceivedAmount: BigDecimal,

    /**
     * ид клиента в системе мерчанта
     */
    val customerId: String,

    /**
     * уникальный order id в системе мерчанта
     */
    val orderId: String,

    val description: String?,
)