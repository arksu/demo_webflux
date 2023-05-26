package com.example.demowebflux.service

import com.example.jooq.tables.pojos.Currency
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ExchangeRateService(
    @Value("\${app.decimalScale:8}")
    private val decimalScale: Int
) {
    fun getRate(from: Currency, to: Currency): BigDecimal {
        if (from.id == to.id) return BigDecimal.ONE

        // TODO
        return BigDecimal("2.452").setScale(decimalScale, RoundingMode.FLOOR)
//        return BigDecimal.ONE
    }
}