package com.example.demowebflux.service

import com.example.jooq.tables.pojos.Currency
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.util.retry.Retry
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

@Service
class ExchangeRateService(
    @Value("\${app.decimalScale:8}")
    private val decimalScale: Int,

    @Value("\${app.rates.supported}")
    private val supportedList: List<String>
) {
    private val webClient = WebClient
        .builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs {
                    it.defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)
                }
                .build()
        )
        .baseUrl("https://www.binance.com/bapi/asset/v2/public/asset-service/product/get-products")
        .build()

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

    @Scheduled(fixedDelayString = "\${app.rates.updateInterval}")
    fun updateBinance() {
        val dto = getRatesFromBinance()
        if (dto != null) {
            val map = dto.data.associate { it.s to it.c }
            supportedList.forEach {
                val rate = map[it]
                if (rate != null) {
                    println("$it=$rate")
                }
            }
        }
    }

    fun getRatesFromBinance(): BinanceRateResponse? {
        return webClient
            .get()
            .retrieve()
            .bodyToMono(BinanceRateResponse::class.java)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
            .block()
    }

    data class BinanceRateResponse(
        val data: List<BinanceRateData>,
        val success: Boolean
    )

    data class BinanceRateData(
        val s: String,
        val st: String,
        val b: String,
        val q: String,
        val an: String,
        val qn: String,
        val pm: String,
        val pn: String,
        val o: BigDecimal,
        val h: BigDecimal,
        val l: BigDecimal,
        val c: BigDecimal,
        val v: BigDecimal,
        val qv: BigDecimal,
    )
}