package com.example.demowebflux.service

import com.example.demowebflux.repo.ShopRepo
import com.example.demowebflux.repo.WebhookRepo
import com.example.demowebflux.repo.WebhookResultRepo
import com.example.demowebflux.service.dto.webhook.OrderWebhook
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.enums.WebhookStatus
import com.example.jooq.tables.pojos.Invoice
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.pojos.Webhook
import com.example.jooq.tables.pojos.WebhookResult
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class WebhookService(
    private val shopService: ShopService,
    private val currencyService: CurrencyService,
    private val mapper: ObjectMapper,
    private val webhookRepo: WebhookRepo,
    private val shopRepo: ShopRepo,
    private val dslContext: DSLContext,
    private val webhookClient: WebClient,
    private val webhookResultRepo: WebhookResultRepo,
) {
    val log by LoggerDelegate()

    /**
     * добавить задание на отправку вебхука мерчанту
     */
    suspend fun submit(order: Order, invoice: Invoice, context: DSLContext) {
        val shop = shopService.getById(invoice.shopId, context)
        val invoiceCurrency = currencyService.getById(invoice.currencyId)
        val selectedCurrency = currencyService.getById(order.selectedCurrencyId)

        val webhookBody = OrderWebhook(
            merchantId = shop.merchantId,
            shopId = invoice.shopId,
            apiKey = shop.apiKey,
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
        webhook.status = WebhookStatus.NEW
        webhook.errorCount = 0
        webhook.signature = getSignature(webhook)
        webhook.dueDate = null

        webhookRepo.save(webhook, context).awaitSingle()
    }

    fun makeWebhookBody(webhook: Webhook): OrderWebhook {
        return mapper.readValue(webhook.requestBody, OrderWebhook::class.java)
    }

    @Scheduled(fixedDelay = 1000)
    fun process() {
        mono {
            dslContext.transactionCoroutine { trx ->
                val context = trx.dsl()
                // ищем не отправленные вебхуки
                val list = webhookRepo.findAllIsNotCompletedLimitForUpdateSkipLocked(100, context).collectList().awaitFirst()

                Flux.fromIterable(list)
                    .flatMap { webhook ->
                        mono {
                            try {
                                processWebhook(webhook, context)
                            } catch (t: Throwable) {
                                log.error("Failed process webhook $webhook", t)
                            }
                        }
                    }
                    .collectList()
                    .awaitFirst()
            }
        }.timeout(Duration.ofSeconds(60)).subscribe()
    }

    suspend fun processWebhook(webhook: Webhook, context: DSLContext) {
        val startTime = System.currentTimeMillis()

        // send webhook
        webhookClient
            .post()
            .uri(webhook.url)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Signature", webhook.signature)
            .bodyValue(makeWebhookBody(webhook))
            .exchangeToMono { response ->
                val endTime = System.currentTimeMillis()
                mono {
                    if (response.statusCode().is2xxSuccessful) {
                        webhookRepo.setCompleted(webhook.id, context).awaitSingle()
                    } else {
                        incErrorCount(webhook)
                    }
                    saveResult(webhook, response, webhook.errorCount + 1, endTime - startTime)
                }
            }
            .onErrorResume { throwable ->
                val endTime = System.currentTimeMillis()
                mono {
                    incErrorCount(webhook)
                    saveError(webhook, throwable, webhook.errorCount + 1, endTime - startTime)
                    null
                }
            }
            .subscribeOn(Schedulers.parallel())
            .timeout(Duration.ofSeconds(20))
            .awaitSingleOrNull()
    }

    suspend fun getSignature(webhook: Webhook): String {
        val shop = shopRepo.findById(webhook.shopId, dslContext).awaitSingle()
        val secretKey = shop.secretKey
        val hmac = Mac.getInstance("HmacSHA512")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA512")
        hmac.init(secretKeySpec)
        val hash = hmac.doFinal(webhook.requestBody.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    suspend fun incErrorCount(webhook: Webhook) {
        dslContext.transactionCoroutine { trx ->
            val w = webhookRepo.incErrorCount(webhook, trx.dsl()).awaitSingle()
            if (w.errorCount > 5) {
                webhookRepo.setError(webhook.id, trx.dsl()).awaitSingle()
            }
        }
    }

    suspend fun saveResult(webhook: Webhook, response: ClientResponse, tryNum: Int, durationMs: Long): WebhookResult {
        val result = WebhookResult()
        result.webhookId = webhook.id
        result.responseCode = response.statusCode().value()
        result.tryNum = tryNum
        result.responseTime = durationMs.toInt()
        result.responseBody = response.bodyToMono(String::class.java).defaultIfEmpty("").awaitSingleOrNull()
        return webhookResultRepo.save(result, dslContext).awaitSingle()
    }

    suspend fun saveError(webhook: Webhook, throwable: Throwable, tryNum: Int, durationMs: Long) {
        val result = WebhookResult()
        result.webhookId = webhook.id
        result.responseCode = 0
        result.tryNum = tryNum
        result.responseTime = durationMs.toInt()
        result.responseBody = throwable.javaClass.simpleName + ": " + throwable.message
        webhookResultRepo.save(result, dslContext).awaitSingle()
    }
}