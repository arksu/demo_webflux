package com.example.demowebflux.service

import com.example.demowebflux.exception.WebhookException
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
import org.jooq.DSLContext
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
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
        // ищем не отправленные вебхуки
        webhookRepo.findAllIsNotCompletedLimit(100, dslContext)
            // log try
            .flatMap { webhook ->
                // увеличиваем счетчик попыток
                webhookRepo.incTryCount(webhook, dslContext)
                    .then(
                        // send webhook
                        webClient.post()
                            .uri(webhook.url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(makeWebhookBody(webhook))
                            .exchangeToMono { response ->
                                val responseCode = response.statusCode().value()

                                if (response.statusCode().is2xxSuccessful) {
                                    webhookRepo.setCompleted(webhook.id, dslContext)
                                        .then(
                                            response.bodyToMono(String::class.java)
                                                .flatMap { responseString ->
                                                    val result = WebhookResult()
                                                        .setWebhookId(webhook.id)
                                                        .setResponseBody(responseString)
                                                        .setTryNum(0)
                                                        .setResponseTime(0)
                                                    webhookResultRepo.save(result, dslContext)
                                                }
                                        )
                                } else {
                                    saveError(webhook, response).then(
                                        // Handle non-successful HTTP status codes here
                                        Mono.error(WebhookException(responseCode, webhook.id))
                                    )
                                }
                            }
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

    fun saveError(webhook: Webhook, response: ClientResponse): Mono<WebhookResult> {
        val result = WebhookResult()
        result.webhookId = webhook.id
        result.tryNum = 1 // TODO
        result.responseTime = 0 // TODO
//        result.error = response.bodyToMono(String::class.java).awaitSingleOrNull()
        return webhookResultRepo.save(result, dslContext)
    }
}