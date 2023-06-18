package com.example.demowebflux.converter

import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.repo.BlockchainTransactionPendingRepo
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.service.ShopService
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Order
import kotlinx.coroutines.reactor.awaitSingle
import org.jooq.DSLContext
import org.springframework.stereotype.Component


@Component
class InvoiceDTOConverter(
    private val currencyService: CurrencyService,
    private val shopService: ShopService,
    private val blockchainTransactionPendingRepo: BlockchainTransactionPendingRepo,
    private val dslContext: DSLContext,
) {
    suspend fun toInvoiceResponseDTO(invoice: Invoice, order: Order?, wallet: BlockchainIncomeWallet?): InvoiceResponseDTO {
        val currency = if (order != null) currencyService.getById(order.selectedCurrencyId) else null
        val shop = shopService.getById(invoice.shopId, dslContext)

        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        val status = when (invoice.status) {
            InvoiceStatusType.NEW -> OrderStatusType.NEW

            InvoiceStatusType.PROCESSING -> {
                order?.status ?: OrderStatusType.ERROR
            }

            InvoiceStatusType.TERMINATED -> order?.status ?: OrderStatusType.EXPIRED
        }

        val isBlockchainTransactionInProcess = if (order != null) {
            val count = blockchainTransactionPendingRepo.findCountNotCompletedByOrderId(order.id, dslContext).awaitSingle()
            count > 0
        } else false

        return InvoiceResponseDTO(
            invoiceId = invoice.externalId,
            invoiceAmount = invoice.amount,
            invoiceCurrency = currencyService.getById(invoice.currencyId).name,
            shopName = shop.name,
            deadline = invoice.deadline.toString(),
            status = status,
            walletAddress = wallet?.address,
            currency = currency?.name,
            amount = order?.customerAmount,
            amountReceived = order?.customerAmountReceived,
            amountPending = order?.customerAmountPending,
            shopUrl = shop.url,
            successUrl = invoice.successUrl,
            failUrl = invoice.failUrl,
            isBlockchainTransactionInProcess = isBlockchainTransactionInProcess
        )
    }
}