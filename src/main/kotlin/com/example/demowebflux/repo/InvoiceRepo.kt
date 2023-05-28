package com.example.demowebflux.repo

import com.example.jooq.Tables.INVOICE
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.records.InvoiceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class InvoiceRepo : AbstractCrudRepo<UUID, Invoice, InvoiceRecord>(
    INVOICE, INVOICE.ID, ::Invoice
) {
    fun findByIdForUpdateSkipLocked(id: UUID, context: DSLContext): Mono<Invoice> {
        return context.selectFrom(INVOICE)
            .where(INVOICE.ID.eq(id))
            .forUpdate()
            .skipLocked()
            .toMono()
            .map(::Invoice)
    }

    fun updateStatus(invoice: Invoice, context: DSLContext): Mono<Int> {
        return context.update(INVOICE)
            .set(INVOICE.STATUS, invoice.status)
            .where(INVOICE.ID.eq(invoice.id))
            .toMono()
    }
}