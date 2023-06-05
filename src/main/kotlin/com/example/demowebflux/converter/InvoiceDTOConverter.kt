package com.example.demowebflux.converter

import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.service.CurrencyService
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Order
import org.springframework.stereotype.Component


@Component
class InvoiceDTOConverter(
    private val currencyService: CurrencyService
) {
    fun toInvoiceResponseDTO(invoice: Invoice, order: Order?, wallet: BlockchainIncomeWallet?): InvoiceResponseDTO {
        val currency = if (order != null) currencyService.getById(order.selectedCurrencyId) else null
        return InvoiceResponseDTO(
            invoiceId = invoice.externalId,
            invoiceAmount = invoice.amount,
            invoiceCurrency = currencyService.getById(invoice.currencyId).name,
            status = order?.status ?: OrderStatusType.NEW,
            walletAddress = wallet?.address,
            currency = currency?.name,
            amount = order?.customerAmount,
            amountReceived = order?.customerAmountReceived,
            amountPending = order?.customerAmountPending
        )
    }
}