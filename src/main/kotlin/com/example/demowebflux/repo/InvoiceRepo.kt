package com.example.demowebflux.repo

import com.example.jooq.Tables.INVOICE
import com.example.jooq.tables.pojos.Invoice
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class InvoiceRepo(
    private val dslContext: DSLContext
) {
    fun findByIdForUpdate(id: UUID, context: DSLContext): Mono<Invoice> {
        return context.selectFrom(INVOICE)
            .where(INVOICE.ID.eq(id))
            .forUpdate()
            .skipLocked()
            .toMono()
            .map(::Invoice)
    }

    fun save(invoice: Invoice): Mono<Invoice> {
        return dslContext.insertInto(INVOICE)
            .set(INVOICE.MERCHANT_ID, invoice.merchantId)
            .set(INVOICE.CUSTOMER_ID, invoice.customerId)
            .set(INVOICE.MERCHANT_ORDER_ID, invoice.merchantOrderId)
            .set(INVOICE.CURRENCY_ID, invoice.currencyId)
            .set(INVOICE.AMOUNT, invoice.amount)
            .set(INVOICE.COMMISSION, invoice.commission)
            .set(INVOICE.DESCRIPTION, invoice.description)
            .set(INVOICE.SUCCESS_URL, invoice.successUrl)
            .set(INVOICE.FAIL_URL, invoice.failUrl)
            .returning()
            .toMono()
            .map(::Invoice)
    }
}