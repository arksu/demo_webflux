package com.example.demowebflux

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.controller.dto.MerchantInvoiceResponseDTO
import com.example.demowebflux.repo.*
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.util.randomStringByKotlinRandom
import com.example.jooq.enums.CommissionType
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.pojos.Merchant
import com.example.jooq.tables.pojos.Shop
import com.github.javafaker.Faker
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class WebTests(
    @Value("\${app.decimalScale}")
    val decimalScale: Int
//    @LocalServerPort
//    val port: Int,
) {
    private val faker = Faker()

    private val defaultMerchantId: UUID = UUID.fromString("2a3e59ff-b549-4ca2-979c-e771c117f350")
    private val defaultApiKey = "XXuMTye9BpV8yTYYtK2epB452p9PgcHgHK3CDGbLDGwc4xNmWT7y2wmVTKtGvwyZ"
    private val defaultSecretKey = "DVU2bHga73qyv5rR4pAWCFzktxun6dhcMPBNZKSE8J"
    private val defaultWebhookUrl = "http://localhost:8095"

    private val defaultCurrency = "USDT-TRC20"

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var currencyService: CurrencyService

    @Autowired
    private lateinit var invoiceRepo: InvoiceRepo

    @Autowired
    private lateinit var orderRepo: OrderRepo

    @Autowired
    private lateinit var merchantRepo: MerchantRepo

    @Autowired
    private lateinit var shopRepo: ShopRepo

    @Autowired
    private lateinit var blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo

    @Autowired
    private lateinit var webClient: WebTestClient

    companion object {
        @Container
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:14.7-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }

            val r2dbcUrl = postgres.jdbcUrl.replace("jdbc:", "r2dbc:")
            registry.add("spring.r2dbc.url") { r2dbcUrl }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
        }
    }

    @Test
    fun testWrongInvoice() {
        // сумма 0
        val invoiceRequest = InvoiceRequestDTO(
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "USDT-TRC20-NILE",
            BigDecimal(0),
            null,
            "https://google.com",
            "https://google.com/fail",
        )

        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest))
            .exchange()
            .expectStatus().isBadRequest

        // сумма
        val invoiceRequest2 = InvoiceRequestDTO(
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "USDT-TRC20-NILE",
            BigDecimal(-1),
            null,
            "https://google.com",
            "https://google.com/fail",
        )

        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest2))
            .exchange()
            .expectStatus().isBadRequest

        // валюта
        val invoiceRequest3 = InvoiceRequestDTO(
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "TRX2",
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
        )

        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest3))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun testCreateWrongApiKeyInvoice() {
        val invoiceRequest = InvoiceRequestDTO(
            "wrong_api_key",
            "some_customer_id",
            "order#112",
            "USDT-TRC20-NILE",
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
        )

        // создаем инвойс
        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun testCreateInvoice() {
        val invoiceRequest = InvoiceRequestDTO(
            defaultApiKey,
            "some_customer_id",
            "order#1",
            defaultCurrency,
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
        )

        // создаем инвойс
        val response = createInvoice(invoiceRequest)

        runBlocking {
            // прочитаем его из базы. он там должен быть
            val invoice = invoiceRepo.findByExternalId(response.id, dslContext).awaitSingleOrNull()
            assertNotNull(invoice)
            assertEquals(invoice?.customerId, "some_customer_id")
        }

        // создаем еще один такой же - нарушается уникальность orderId, запрос должен упасть
        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    fun createInvoice(request: InvoiceRequestDTO): MerchantInvoiceResponseDTO {
        val result = webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .returnResult(MerchantInvoiceResponseDTO::class.java)
            .responseBody

        val response = runBlocking {
            result.awaitSingle()
        }
        return response
    }

    fun createOrder(request: CreateOrderRequestDTO): InvoiceResponseDTO {
        val result = webClient.post()
            .uri("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .returnResult(InvoiceResponseDTO::class.java)
            .responseBody

        val response = runBlocking {
            result.awaitSingle()
        }
        return response
    }

    fun createFreeWallet(currencyName: String): BlockchainIncomeWallet {

        val wallet = BlockchainIncomeWallet()
        wallet.address = UUID.randomUUID().toString()
        wallet.currencyId = currencyService.getByName(currencyName).id
        wallet.orderId = null
        wallet.isGenerated = false
        return runBlocking {
            blockchainIncomeWalletRepo.disableAll(dslContext).awaitSingle()
            blockchainIncomeWalletRepo.save(wallet, dslContext).awaitSingle()
        }
    }

    fun createOrderWithNumber(
        merchantId: UUID,
        apiKey: String,
        num: Int,
        sum: BigDecimal = BigDecimal(100),
        selectedCurrency: String = defaultCurrency
    ): InvoiceResponseDTO {
        val invoiceRequest = InvoiceRequestDTO(
            apiKey = apiKey,
            customerId = "some_customer_id",
            orderId = "order#$num",
            currency = defaultCurrency,
            amount = sum,
            description = null,
            successUrl = "https://google.com",
            failUrl = "https://google.com/fail",
        )

        // создаем инвойс
        val invoiceResponse = createInvoice(invoiceRequest)
        runBlocking {
            val invoice = invoiceRepo.findByExternalId(invoiceResponse.id, dslContext).awaitSingle()
            assertNotNull(invoice)
            assertEquals(invoice.status, InvoiceStatusType.NEW)
        }

        val request = CreateOrderRequestDTO(
            invoiceResponse.id, selectedCurrency
        )
        // создаем ордер на инвойс
        val response = createOrder(request)
        assertNotNull(response)

        val invoice = runBlocking {
            invoiceRepo.findByExternalId(response.invoiceId, dslContext).awaitSingle()
        }
        assertNotNull(invoice)
        assertEquals(invoice.status, InvoiceStatusType.PROCESSING)

        // убедимся что созданный заказ есть в базе
        val order = runBlocking { orderRepo.findByInvoiceId(invoice.id, dslContext).awaitSingle() }
        assertEquals(order.status, OrderStatusType.PENDING)

        // а также что наш кошелек заблокировался
        // ищем кошелк по ид ордера
        val updatedWallet = runBlocking { blockchainIncomeWalletRepo.findByOrderId(order.id, dslContext).awaitSingleOrNull() }
        assertNotNull(updatedWallet)
        assertEquals(order.id, updatedWallet!!.orderId)

        return response
    }

    suspend fun createMerchantAndShop(commission: BigDecimal, allowRecalculation: Boolean, type: CommissionType): Shop {
        val merch = merchantRepo.save(
            Merchant()
                .setCommission(commission)
                .setLogin(faker.name().username())
                .setEmail(faker.internet().emailAddress()),
            dslContext
        ).awaitSingle()

        return shopRepo.save(
            Shop()
                .setApiKey(randomStringByKotlinRandom(64))
                .setName(randomStringByKotlinRandom(12))
                .setUrl(randomStringByKotlinRandom(12))
                .setSecretKey(defaultSecretKey)
                .setWebhookUrl(defaultWebhookUrl)
                .setAllowRecalculation(allowRecalculation)
                .setMerchantId(merch.id)
                .setCommissionType(type),
            dslContext
        ).awaitSingle()
    }

    @Test
    fun testCreateOrder() {
        // #1 занят другим тестом
        createOrderWithNumber(defaultMerchantId, defaultApiKey, 2)
    }


    /**
     * тест расчета суммы когда комиссию платит клиент
     */
    @Test
    fun testOrderCalcClientCommission() {
        val sum = BigDecimal("1353.465").setScale(decimalScale, RoundingMode.HALF_UP)
        val commission = BigDecimal("3.66").setScale(decimalScale, RoundingMode.HALF_UP)

        runBlocking {
            val shop = createMerchantAndShop(commission, false, CommissionType.CLIENT)

            val response = createOrderWithNumber(shop.id, shop.apiKey, 3, sum)

            val invoice = invoiceRepo.findByExternalId(response.invoiceId, dslContext).awaitSingle()
            assertNotNull(invoice)
            assertEquals(invoice.status, InvoiceStatusType.PROCESSING)

            val order = orderRepo.findByInvoiceId(invoice.id, dslContext).awaitSingleOrNull()
            assertNotNull(order)

            if (order != null) {
                assertEquals(order.invoiceAmount.stripTrailingZeros().toPlainString(), "1353.465")
                assertEquals(order.referenceAmount.stripTrailingZeros().toPlainString(), "1353.465")
                assertEquals(order.merchantAmountByOrder.stripTrailingZeros().toPlainString(), "1353.465")
                assertEquals(order.merchantAmountByInvoice.stripTrailingZeros().toPlainString(), "1353.465")

                // клиент заплатит комиссию
                assertEquals(order.customerAmount.stripTrailingZeros().toPlainString(), "1403.001819")
                // и ее размер
                assertEquals(order.commissionAmount.stripTrailingZeros().toPlainString(), "49.536819")

                assertEquals(order.exchangeRate.stripTrailingZeros().toPlainString(), "1")
                assertEquals(order.commission.stripTrailingZeros().toPlainString(), "3.66")
            }
        }
    }

    @Test
    fun testOrderCalcMerchantCommission() {
        val sum = BigDecimal("1353.465").setScale(decimalScale, RoundingMode.HALF_UP)
        val commission = BigDecimal("3.66").setScale(decimalScale, RoundingMode.HALF_UP)

        runBlocking {
            val shop = createMerchantAndShop(commission, false, CommissionType.MERCHANT)

            val response = createOrderWithNumber(shop.id, shop.apiKey, 4, sum)

            val invoice = invoiceRepo.findByExternalId(response.invoiceId, dslContext).awaitSingle()
            assertNotNull(invoice)
            assertEquals(InvoiceStatusType.PROCESSING, invoice.status)

            val order = orderRepo.findByInvoiceId(invoice.id, dslContext).awaitSingleOrNull()
            assertNotNull(order)

            if (order != null) {
                assertEquals(order.invoiceAmount.stripTrailingZeros().toPlainString(), "1353.465")
                assertEquals(order.referenceAmount.stripTrailingZeros().toPlainString(), "1353.465")

                // мерчант заплатит комиссию. а значит получит меньше
                assertEquals(order.merchantAmountByOrder.stripTrailingZeros().toPlainString(), "1303.928181")
                assertEquals(order.merchantAmountByInvoice.stripTrailingZeros().toPlainString(), "1303.928181")

                assertEquals(order.customerAmount.stripTrailingZeros().toPlainString(), "1353.465")
                // и ее размер
                assertEquals(order.commissionAmount.stripTrailingZeros().toPlainString(), "49.536819")

                assertEquals(order.exchangeRate.stripTrailingZeros().toPlainString(), "1")
                assertEquals(order.commission.stripTrailingZeros().toPlainString(), "3.66")
            }
        }
    }

}
