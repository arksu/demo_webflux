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

    fun findLastByName(name : String, context: DSLContext) : Mono<Rate> {
        println("get rate $name")
        return context.selectFrom(RATE)
            .where(RATE.NAME.eq(name))
            .orderBy(RATE.ID.desc())
            .limit(1)
            .toMono()
            .map(::Rate)
    }
}