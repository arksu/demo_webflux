package com.example.demowebflux.repo

import com.example.jooq.Tables.ORDER
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.records.OrderRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
class OrderRepo : AbstractCrudRepo<UUID, Order, OrderRecord>() {
    override val table = ORDER
    override val idField = ORDER.ID
    override val mapper = { it: OrderRecord -> Order(it) }

    fun findByInvoiceId(id: UUID, context: DSLContext): Mono<Order> {
        return context.selectFrom(ORDER)
            .where(ORDER.INVOICE_ID.eq(id))
            .toMonoAndMap()
    }

    fun findByInvoiceIdForUpdateSkipLocked(id: UUID, context: DSLContext): Mono<Order> {
        return context.selectFrom(ORDER)
            .where(ORDER.INVOICE_ID.eq(id))
            .forUpdate()
            .skipLocked()
            .toMonoAndMap()
    }

    fun updateStatus(entity: Order, context: DSLContext): Mono<Order> {
        return context.update(ORDER)
            .set(ORDER.STATUS, entity.status)
            .where(ORDER.ID.eq(entity.id))
            .returning()
            .toMonoAndMap()
    }

    fun update(entity: Order, context: DSLContext): Mono<Order> {
        return context.update(ORDER)
            .set(ORDER.CUSTOMER_AMOUNT_PENDING, entity.customerAmountPending)
            .set(ORDER.CUSTOMER_AMOUNT_RECEIVED, entity.customerAmountReceived)
            .where(ORDER.ID.eq(entity.id))
            .returning()
            .toMonoAndMap()
    }
}