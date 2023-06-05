package com.example.demowebflux.service

import com.example.demowebflux.util.percentToMult
import com.example.jooq.enums.CommissionType
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Shop
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CalcService(
    @Value("\${app.decimalScale}")
    private val scale: Int
) {
    data class CalcModel(
        // сколько берем с клиента, он должен заплатить больше на сумму комиссии
        val customerAmount: BigDecimal,
        // сколько отдаем мерчанту в валюте сделки с учетом комиссий
        val merchantAmountByOrder: BigDecimal,
        // сколько отдаем мерчанту в валюте invoice
        val merchantAmountByInvoice: BigDecimal
    )


    fun calcOrderAmounts(
        invoice: Invoice,
        shop: Shop,
        referenceAmount: BigDecimal,
        commission: BigDecimal
    ): CalcModel {
        val commissionPercentMult = commission.percentToMult(scale)

        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA") // DDL: commissionType not null
        return when (shop.commissionType) {
            CommissionType.CLIENT -> {
                CalcModel(
                    // сколько берем с клиента, он должен заплатить больше на сумму комиссии
                    customerAmount = (referenceAmount * (BigDecimal.ONE + commissionPercentMult)).setScale(scale, RoundingMode.HALF_UP),
                    // сколько отдаем мерчанту в валюте сделки с учетом комиссий
                    merchantAmountByOrder = referenceAmount.setScale(scale, RoundingMode.HALF_UP),
                    // сколько отдаем мерчанту в валюте invoice
                    merchantAmountByInvoice = invoice.amount.setScale(scale, RoundingMode.HALF_UP)
                )
            }

            CommissionType.MERCHANT -> {
                CalcModel(
                    // сколько берем с клиента, он должен ровно столько сколько заявлено
                    customerAmount = referenceAmount.setScale(scale, RoundingMode.HALF_UP),
                    // сколько отдаем мерчанту в валюте сделки с учетом комиссий
                    merchantAmountByOrder = (referenceAmount * (BigDecimal.ONE - commissionPercentMult)).setScale(scale, RoundingMode.HALF_UP),
                    // сколько отдаем мерчанту в валюте invoice - он получит меньше на сумму комиссии
                    merchantAmountByInvoice = (invoice.amount * (BigDecimal.ONE - commissionPercentMult)).setScale(scale, RoundingMode.HALF_UP)
                )
            }
        }

    }
}