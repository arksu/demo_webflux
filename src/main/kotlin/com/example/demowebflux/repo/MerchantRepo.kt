package com.example.demowebflux.repo

import com.example.jooq.Tables.MERCHANT
import com.example.jooq.tables.pojos.Merchant
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class MerchantRepo {
    fun findById(id: UUID, context: DSLContext): Mono<Merchant> {
        return context.selectFrom(MERCHANT)
            .where(MERCHANT.ID.eq(id))
            .toMono()
            .map(::Merchant)
    }

    fun save(entity: Merchant, context: DSLContext): Mono<Merchant> {
        return context.insertInto(MERCHANT)
            .set(context.newRecord(MERCHANT, entity))
            .returning()
            .toMono()
            .map(::Merchant)
    }
}