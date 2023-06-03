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
}