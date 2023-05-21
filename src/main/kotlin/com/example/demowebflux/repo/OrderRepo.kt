package com.example.demowebflux.repo

import com.example.jooq.Tables.ORDER
import com.example.jooq.tables.pojos.Order
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Repository
class OrderRepo(private val dslContext: DSLContext) {
    fun save(entity: Order): Mono<Order> {
        return dslContext.insertInto(ORDER)
            .set(ORDER.INVOICE_ID, entity.invoiceId)
            .set(ORDER.SELECTED_CURRENCY_ID, entity.selectedCurrencyId)
            .set(ORDER.STATUS, entity.status)
            .set(ORDER.CONFIRMATIONS, entity.confirmations)
            .set(ORDER.AMOUNT, entity.amount)
            .returning()
            .toMono()
            .map(::Order)
    }
}