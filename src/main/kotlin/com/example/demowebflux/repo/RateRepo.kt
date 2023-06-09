package com.example.demowebflux.repo

import com.example.jooq.Tables.RATE
import com.example.jooq.tables.pojos.Rate
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Repository
class RateRepo {
    fun save(entity: Rate, context: DSLContext): Mono<Rate> {
        return context.insertInto(RATE)
            .set(RATE.RATE_, entity.rate)
            .set(RATE.NAME, entity.name)
            .set(RATE.SOURCE, entity.source)
            .returning()
            .toMono()
            .map(::Rate)
    }
}