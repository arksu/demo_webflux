package com.example.demowebflux.service

import com.example.demowebflux.exception.NoActualRateException
import com.example.demowebflux.repo.RateRepo
import com.example.jooq.enums.RateSource
import com.example.jooq.tables.pojos.Currency
import com.example.jooq.tables.pojos.Rate
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.util.retry.Retry
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.OffsetDateTime

@Service
class ExchangeRateService(
    @Value("\${app.decimalScale:8}")
    private val decimalScale: Int,

    @Value("\${app.rates.lifetimeSeconds:30}")
    private val lifetimeSeconds: Long,

    @Value("\${app.rates.supported}")
    private val supportedList: List<String>,

    private val rateRepo: RateRepo,
    private val dslContext: DSLContext,
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

        val now = OffsetDateTime.now()
        val rate = rateRepo.findLastByName("${from.name.toRateName()}${to.name.toRateName()}", dslContext).awaitSingleOrNull()
        if (rate != null) {
            return if (rate.created.isBefore(now.minusSeconds(lifetimeSeconds))) {
                throw NoActualRateException("${from.name.toRateName()}${to.name.toRateName()}")
            } else rate.rate
        } else {
            val reverted = rateRepo.findLastByName("${to.name.toRateName()}${from.name.toRateName()}", dslContext).awaitSingleOrNull()
                ?: return null
            val r = if (reverted.created.isBefore(now.minusSeconds(lifetimeSeconds))) {
                throw NoActualRateException("${to.name.toRateName()}${from.name.toRateName()}")
            } else reverted.rate
            return BigDecimal.ONE.divide(r, decimalScale, RoundingMode.HALF_UP)
        }
    }

    fun String.toRateName(): String {
        if (this.startsWith("USDT-")) return "USDT"
        if (this.startsWith("TRX-")) return "TRX"
        return this
    }

    @Scheduled(fixedDelayString = "\${app.rates.updateInterval}")
    fun updateBinance() {
        val dto = getRatesFromBinance()
        if (dto != null) {
            val map = dto.data.associate { it.s to it.c }
            supportedList.forEach {
                val rateValue = map[it]
                if (rateValue != null) {
                    val rate = Rate()
                    rate.rate = rateValue
                    rate.name = it
                    rate.source = RateSource.BINANCE
                    rateRepo.save(rate, dslContext).block()
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