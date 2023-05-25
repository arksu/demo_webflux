package com.example.demowebflux

import com.example.demowebflux.controller.dto.CreateOrderRequestDTO
import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.controller.dto.OrderResponseDTO
import com.example.demowebflux.repo.InvoiceRepo
import com.example.demowebflux.repo.OrderRepo
import com.example.jooq.enums.CommissionType
import com.example.jooq.enums.InvoiceStatusType
import com.example.jooq.enums.OrderStatusType
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
import java.util.*

@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class WebTests(
//    @LocalServerPort
//    val port: Int,
) {
    private val merchantId: UUID = UUID.fromString("2a3e59ff-b549-4ca2-979c-e771c117f350")

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var invoiceRepo: InvoiceRepo

    @Autowired
    private lateinit var orderRepo: OrderRepo

    @Autowired
    private lateinit var webClient: WebTestClient

    companion object {
        @Container
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:14.7-alpine")
            .withReuse(true)

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
        val invoiceRequest = InvoiceRequestDTO(
            merchantId,
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

        val invoiceRequest2 = InvoiceRequestDTO(
            merchantId,
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

        val invoiceRequest3 = InvoiceRequestDTO(
            merchantId,
            "some_customer_id",
            "order#1",
            "TRX",
            BigDecimal(-1),
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
    fun testCreateInvoice() {
        val invoiceRequest = InvoiceRequestDTO(
            merchantId,
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
            val invoice = invoiceRepo.findById(response.id, dslContext).awaitSingleOrNull()
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

    fun createOrderWithNumber(num: Int): OrderResponseDTO {
        val invoiceRequest = InvoiceRequestDTO(
            merchantId,
            "some_customer_id",
            "order#$num",
            "USDT-TRC20-NILE",
            BigDecimal(100),
            null,
            "https://google.com",
            "https://google.com/fail",
            CommissionType.CLIENT
        )

        // создаем инвойс
        val invoiceResponse = createInvoice(invoiceRequest)
        runBlocking {
            val invoice = invoiceRepo.findById(invoiceResponse.id, dslContext).awaitSingleOrNull()
            assertNotNull(invoice)
            assertEquals(invoice?.status, InvoiceStatusType.NEW)
        }

        val request = CreateOrderRequestDTO(
            invoiceResponse.id, "USDT-TRC20-NILE"
        )
        val response = createOrder(request)
        assertNotNull(response)

        runBlocking {
            val invoice = invoiceRepo.findById(invoiceResponse.id, dslContext).awaitSingleOrNull()
            assertNotNull(invoice)
            assertEquals(invoice?.status, InvoiceStatusType.PROCESSING)
        }

        return response
    }

    @Test
    fun testCreateOrder() {
        // #1 занят другим тестом
        val response = createOrderWithNumber(2)
        runBlocking {
            val order = orderRepo.findById(response.id, dslContext).awaitSingleOrNull()
            assertNotNull(order)
            assertEquals(order?.status, OrderStatusType.NEW)
        }
    }

}
