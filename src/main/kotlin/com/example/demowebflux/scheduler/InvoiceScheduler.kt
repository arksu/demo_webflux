package com.example.demowebflux.scheduler

import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.service.InvoiceService
import com.example.demowebflux.service.OrderService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

@Service
class InvoiceScheduler(
    private val invoiceRepo: InvoiceRepo,
    private val dslContext: DSLContext,
    private val orderService: OrderService,
    private val invoiceService: InvoiceService,
) {
    @Scheduled(fixedDelay = 1000)
    fun processInvoices() {
        val now = OffsetDateTime.now()

        val flux = invoiceRepo.findAllNotTerminated(dslContext).flatMap { invoice ->
            if (invoice.deadline.isBefore(now)) {
                mono {
                    dslContext.transactionCoroutine { trx ->
                        val context = trx.dsl()
                        val lockedInvoice = invoiceRepo.findByIdForUpdateSkipLocked(invoice.id, context).awaitSingleOrNull()
                        if (lockedInvoice != null) {
                            invoiceService.expire(lockedInvoice, context)
                        }
                    }
                }
            } else {
                Mono.empty()
            }
        }
        flux.blockLast()
    }
}