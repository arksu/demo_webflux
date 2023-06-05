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
    /**
     * получить курс перевода валюты FROM в валюту TO
     * берем FROM * rate = TO
     * то есть это просто множитель для FROM
     * либо null - если конвертация в принципе невозможна
     */
    suspend fun getRate(from: Currency, to: Currency): BigDecimal? {
        if (from.id == to.id) return BigDecimal.ONE

        // надо учесть дату последнего обновления курса в кэше
        // если курс протух то обязательно ждем актуальный курс, либо кидаем ошибку

        if (from.name == "USDD-TRC20-NILE") return null


        // TODO
        return BigDecimal("2.452").setScale(decimalScale, RoundingMode.HALF_UP)
//        return BigDecimal.ONE
    }
}