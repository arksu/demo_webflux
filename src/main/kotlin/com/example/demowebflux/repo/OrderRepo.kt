package com.example.demowebflux.repo

import com.example.jooq.Tables.ORDER
import com.example.jooq.tables.pojos.Order
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class OrderRepo(private val dslContext: DSLContext) {

    fun findById(id: UUID, context: DSLContext): Mono<Order> {
        return context.selectFrom(ORDER)
            .where(ORDER.ID.eq(id))
            .toMono()
            .map(::Order)
    }

    fun save(entity: Order, context: DSLContext): Mono<Order> {
        return context.insertInto(ORDER)
            .set(dslContext.newRecord(ORDER, entity))
            .returning()
            .toMono()
            .map(::Order)
    }
}