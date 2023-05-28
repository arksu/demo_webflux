package com.example.demowebflux.service

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.util.LoggerDelegate
import com.example.demowebflux.util.percentToMult
import com.example.jooq.enums.CommissionType
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.Order
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@Service
class OrderService(
    private val currencyService: CurrencyService,
    private val invoiceRepo: InvoiceRepo,
    private val orderRepo: OrderRepo,
    private val dslContext: DSLContext,
    private val exchangeRateService: ExchangeRateService,
    private val merchantService: MerchantService,
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
) {
    val log by LoggerDelegate()

    /**
     * клиент выбрал валюту, запускаем сделку, выбираем кошелек под эту валюту
     */
    suspend fun startOrder(request: CreateOrderRequestDTO): Order {
        return dslContext.transactionCoroutine { trx ->
            val invoice = invoiceRepo.findByIdForUpdateSkipLocked(request.invoiceId, trx.dsl()).awaitSingleOrNull()
                ?: throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "Invoice is not found or locked")

            if (invoice.status != InvoiceStatusType.NEW) {
                throw ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Wrong invoice status")
            }
            invoice.status = InvoiceStatusType.PROCESSING
            invoiceRepo.updateStatus(invoice, trx.dsl()).awaitFirst()

            val merchant = merchantService.getById(invoice.merchantId, trx.dsl())

            val fromCurrency = currencyService.getById(invoice.currencyId)
            val targetCurrency = currencyService.getByName(request.selectedCurrency)
            val exchangeRate = exchangeRateService.getRate(fromCurrency, targetCurrency)

            // ищем свободные кошельки на которые можем принять
            val freeWallets = blockchainIncomeWalletRepo.findByCurrencyAndFree(targetCurrency.id, trx.dsl()).awaitSingleOrNull()?.shuffled()
                ?: throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "No free wallet now")

            if (freeWallets.isEmpty()) {
                throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "No free wallet now")
            }
            // берем первый из перемешанного списка свободных кошельков
            val wallet = freeWallets[0]

            val new = Order()
            new.status = OrderStatusType.NEW
            new.invoiceId = invoice.id
            new.invoiceAmount = invoice.amount
            new.incomeWalletId = wallet.id

            // эталонная сумма сделки в валюте которую выбрал клиент, от которой идет расчет (invoice.amount -> exchange_rate[selected_currency_id])
            new.referenceAmount = invoice.amount * exchangeRate
            // сколько фактически пришло от клиента (может он отправил больше чем надо)
            new.customerAmountFact = BigDecimal.ZERO
            new.commission = merchant.commission

            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA") // DDL
            when (invoice.commissionType) {
                CommissionType.CLIENT -> {
                    // сколько берем с клиента, он должен заплатить больше на сумму комиссии
                    new.customerAmount = new.referenceAmount * (BigDecimal.ONE + merchant.commission.percentToMult())
                    // сколько отдаем мерчанту в валюте сделки с учетом комиссий
                    new.merchantAmountOrder = new.referenceAmount
                    // сколько отдаем мерчанту в валюте invoice
                    new.merchantAmount = invoice.amount
                }

                CommissionType.MERCHANT -> {
                    // сколько берем с клиента, он должен ровно столько сколько заявлено
                    new.customerAmount = new.referenceAmount
                    // сколько отдаем мерчанту в валюте сделки с учетом комиссий
                    new.merchantAmountOrder = new.referenceAmount * (BigDecimal.ONE - merchant.commission.percentToMult())
                    // сколько отдаем мерчанту в валюте invoice - он получит меньше на сумму комиссии
                    new.merchantAmount = invoice.amount * (BigDecimal.ONE - merchant.commission.percentToMult())
                }
            }

            // сумма комиссии которую взимаем с мерчанта
            new.commissionAmount = new.customerAmount - new.merchantAmountOrder
            if (new.commissionAmount < BigDecimal.ZERO) {
                throw ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Commission could not be negative")
            }

            new.exchangeRate = exchangeRate
            new.selectedCurrencyId = targetCurrency.id
            new.confirmations = 0

            val save = orderRepo.save(new, trx.dsl()).awaitFirst()

            save
        }
    }
}