package com.example.demowebflux.repo

import com.example.jooq.Tables.CURRENCY
import com.example.jooq.tables.pojos.Currency
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class CurrencyRepo(
    private val dslContext: DSLContext
) {
    fun loadAll(): Flux<Currency> {
        return Flux.from(
            dslContext.selectFrom(CURRENCY)
        ).map(::Currency)
    }
}