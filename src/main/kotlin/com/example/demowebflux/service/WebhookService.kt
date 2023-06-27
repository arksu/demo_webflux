package com.example.demowebflux.service

import com.example.demowebflux.repo.WebhookRepo
import com.example.demowebflux.repo.WebhookResultRepo
import com.example.demowebflux.service.dto.webhook.OrderWebhook
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.pojos.Webhook
import com.example.jooq.tables.pojos.WebhookResult
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class WebhookService(
    private val shopService: ShopService,
    private val currencyService: CurrencyService,
    private val mapper: ObjectMapper,
    private val webhookRepo: WebhookRepo,
    private val dslContext: DSLContext,
    private val webClient: WebClient,
    private val webhookResultRepo: WebhookResultRepo,
) {
    val log by LoggerDelegate()

    /**
     * добавить задание на отправку вебхука мерчанту
     */
    suspend fun send(order: Order, invoice: Invoice, context: DSLContext) {
        val shop = shopService.getById(invoice.shopId, context)
        val invoiceCurrency = currencyService.getById(invoice.currencyId)
        val selectedCurrency = currencyService.getById(order.selectedCurrencyId)

        val webhookBody = OrderWebhook(
            merchantId = shop.merchantId,
            shopId = invoice.shopId,
            shopName = shop.name,
            invoiceId = invoice.externalId,
            status = order.status,
            currency = invoiceCurrency.name,
            amount = invoice.amount,
            exchangeRate = order.exchangeRate,
            orderCurrency = selectedCurrency.name,
            orderAmount = order.customerAmount,
            orderPendingAmount = order.customerAmountPending,
            orderReceivedAmount = order.customerAmountReceived,
            customerId = invoice.customerId,
            orderId = invoice.merchantOrderId,
            description = invoice.description
        )

        val webhook = Webhook()
        webhook.shopId = shop.id
        webhook.url = shop.webhookUrl
        webhook.requestBody = mapper.writeValueAsString(webhookBody)
        webhook.isCompleted = false
        webhook.tryCount = 0
        webhook.errorCount = 0

        webhookRepo.save(webhook, context).awaitSingle()
    }

    fun makeWebhookBody(webhook: Webhook): OrderWebhook {
        return mapper.readValue(webhook.requestBody, OrderWebhook::class.java)
    }

    @Scheduled(fixedDelay = 1000)
    fun process() {
        webhookRepo.findAllIsNotCompleted(dslContext)
            // log try
            .flatMap { webhook ->
                webhookRepo.incTryCount(webhook, dslContext)
                    .then(
                        // send webhook
                        webClient.post()
                            .uri(webhook.url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(makeWebhookBody(webhook)))
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .onErrorResume {
                                webhookRepo.incErrorCount(webhook, dslContext)
                                    .then(
                                        Mono.error(RuntimeException("Webhook send error ${it.message}"))
                                    )
                            }
                    )
            }
            .parallel()
            .sequential()
            .blockLast()
    }

    fun saveError(webhook: Webhook, response: ClientResponse): Mono<Void> {
        val result = WebhookResult()
        result.webhookId = webhook.id
        // TODO
        result.error = response.bodyToMono(String::class.java).awaitSingleOrNull()
        return webhookResultRepo.save(dslContext).then()
    }
}