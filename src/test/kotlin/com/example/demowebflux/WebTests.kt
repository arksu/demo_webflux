package com.example.demowebflux

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.controller.dto.OrderResponseDTO
import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.MerchantRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.util.randomStringByKotlinRandom
import com.example.jooq.enums.CommissionType
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import com.example.jooq.tables.pojos.BlockchainIncomeWallet
import com.example.jooq.tables.pojos.Merchant
import com.github.javafaker.Faker
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
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

    private val defaultCurrency = "USDT-TRC20-NILE"

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
            defaultMerchantId,
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "USDT-TRC20-NILE",
            BigDecimal(0),
            null,
            "https://google.com",
            "https://google.com/fail",
            CommissionType.CLIENT
        )

        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest))
            .exchange()
            .expectStatus().isBadRequest

        // сумма
        val invoiceRequest2 = InvoiceRequestDTO(
            defaultMerchantId,
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "USDT-TRC20-NILE",
            BigDecimal(-1),
            null,
            "https://google.com",
            "https://google.com/fail",
            CommissionType.CLIENT
        )

        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest2))
            .exchange()
            .expectStatus().isBadRequest

        // валюта
        val invoiceRequest3 = InvoiceRequestDTO(
            defaultMerchantId,
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "TRX2",
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
            CommissionType.CLIENT
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
            defaultMerchantId,
            "123123",
            "some_customer_id",
            "order#112",
            "USDT-TRC20-NILE",
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
            CommissionType.CLIENT
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
            defaultMerchantId,
            defaultApiKey,
            "some_customer_id",
            "order#1",
            "USDT-TRC20-NILE",
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
            CommissionType.CLIENT
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
            .expectStatus().isBadRequest
    }

    fun createInvoice(request: InvoiceRequestDTO): InvoiceResponseDTO {
        val result = webClient.post()
            .uri("/invoice")
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

    fun createOrder(request: CreateOrderRequestDTO): OrderResponseDTO {
        val result = webClient.post()
            .uri("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .returnResult(OrderResponseDTO::class.java)
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
        return runBlocking {
            blockchainIncomeWalletRepo.save(wallet, dslContext).awaitSingle()
        }
    }

    fun createOrderWithNumber(
        merchantId: UUID,
        apiKey: String,
        num: Int,
        sum: BigDecimal = BigDecimal(100),
        commissionType: CommissionType = CommissionType.CLIENT,
        selectedCurrency: String = defaultCurrency
    ): OrderResponseDTO {
        val wallet = createFreeWallet(defaultCurrency)

        val invoiceRequest = InvoiceRequestDTO(
            merchantId,
            apiKey,
            "some_customer_id",
            "order#$num",
            defaultCurrency,
            sum,
            null,
            "https://google.com",
            "https://google.com/fail",
            commissionType
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
        val response = createOrder(request)
        assertNotNull(response)

        runBlocking {
            // убедимся что созданный заказ есть в базе
            val order = orderRepo.findById(response.id, dslContext).awaitSingle()
            assertEquals(order.status, OrderStatusType.NEW)

            // а также что наш кошелек заблокировался
            val updatedWallet = blockchainIncomeWalletRepo.findById(wallet.id, dslContext).awaitSingle()
            assertEquals(order.id, updatedWallet.orderId)
        }

        return response
    }

    suspend fun createMerchant(commission: BigDecimal): Merchant {
        return merchantRepo.save(
            Merchant()
                .setApiKey(randomStringByKotlinRandom(64))
                .setCommission(commission)
                .setLogin(faker.name().username())
                .setEmail(faker.internet().emailAddress()),
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
        val sum = BigDecimal("1353.465").setScale(decimalScale, RoundingMode.FLOOR)
        val commission = BigDecimal("3.66").setScale(decimalScale, RoundingMode.FLOOR)

        runBlocking {
            val merch = createMerchant(commission)

            val response = createOrderWithNumber(merch.id, merch.apiKey, 3, sum, CommissionType.CLIENT)

            val order = orderRepo.findById(response.id, dslContext).awaitSingleOrNull()
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
        val sum = BigDecimal("1353.465").setScale(decimalScale, RoundingMode.FLOOR)
        val commission = BigDecimal("3.66").setScale(decimalScale, RoundingMode.FLOOR)

        runBlocking {
            val merch = createMerchant(commission)

            val response = createOrderWithNumber(merch.id, merch.apiKey, 4, sum, CommissionType.MERCHANT)

            val order = orderRepo.findById(response.id, dslContext).awaitSingleOrNull()
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
