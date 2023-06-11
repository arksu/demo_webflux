package com.example.demowebflux.repo

import com.example.demowebflux.util.toFlux
import com.example.jooq.Tables.INVOICE
import com.example.jooq.Tables.ORDER
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.records.OrderRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
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

    fun findAllNotTerminated(context: DSLContext): Flux<Pair<Order, Invoice>> {
        return context.select(ORDER, INVOICE)
            .from(ORDER)
            .leftJoin(INVOICE).on(INVOICE.ID.eq(ORDER.INVOICE_ID))
            .where(ORDER.STATUS.`in`(OrderStatusType.PENDING))
            .limit(1000)
            .toFlux()
            .map {
                Order(it.value1()) to Invoice(it.value2())
            }
    }

    fun updateStatus(entity: Order, context: DSLContext): Mono<Order> {
        return context.update(ORDER)
            .set(ORDER.STATUS, entity.status)
            .where(ORDER.ID.eq(entity.id))
            .returning()
            .toMonoAndMap()
    }
}