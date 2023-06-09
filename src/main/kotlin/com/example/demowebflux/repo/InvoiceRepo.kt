package com.example.demowebflux.repo

import com.example.jooq.Tables.INVOICE
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.records.InvoiceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class InvoiceRepo : AbstractCrudRepo<UUID, Invoice, InvoiceRecord>() {
    override val table = INVOICE
    override val idField = INVOICE.ID
    override val mapper = { it: InvoiceRecord -> Invoice(it) }

    fun findByExternalId(id: String, context: DSLContext): Mono<Invoice> {
        return context.selectFrom(INVOICE)
            .where(INVOICE.EXTERNAL_ID.eq(id))
            .toMonoAndMap()
    }

    fun findByExternalIdForUpdateSkipLocked(id: String, context: DSLContext): Mono<Invoice> {
        return context.selectFrom(INVOICE)
            .where(INVOICE.EXTERNAL_ID.eq(id))
            .forUpdate()
            .skipLocked()
            .toMonoAndMap()
    }

    fun updateStatus(invoice: Invoice, context: DSLContext): Mono<Int> {
        return context.update(INVOICE)
            .set(INVOICE.STATUS, invoice.status)
            .where(INVOICE.ID.eq(invoice.id))
            .toMono()
    }

    fun findAllNotTerminated(context: DSLContext): Flux<Invoice> {
        return context.selectFrom(INVOICE)
            .where(INVOICE.STATUS.notEqual(InvoiceStatusType.TERMINATED))
            .limit(1000)
            .toFluxAndMap()
    }

}