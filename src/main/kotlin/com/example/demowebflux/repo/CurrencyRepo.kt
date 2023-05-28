package com.example.demowebflux.repo

import com.example.demowebflux.util.toFlux
import com.example.jooq.Tables.CURRENCY
import com.example.jooq.tables.pojos.Currency
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class CurrencyRepo {
    fun loadAll(context: DSLContext): Flux<Currency> {
        return context.selectFrom(CURRENCY)
            .toFlux()
            .map(::Currency)
    }
}