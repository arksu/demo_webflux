package com.example.demowebflux

import com.example.demowebflux.controller.dto.InvoiceRequestDTO
import com.example.demowebflux.controller.dto.InvoiceResponseDTO
import com.example.demowebflux.repo.AccountRepo
import com.example.demowebflux.repo.InvoiceRepo
import com.example.jooq.enums.CommissionType
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
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
    @LocalServerPort
    val port: Int,
) {
    val merchantId = UUID.fromString("2a3e59ff-b549-4ca2-979c-e771c117f350")

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var accountRepo: AccountRepo

    @Autowired
    private lateinit var invoiceRepo: InvoiceRepo

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
    fun contextLoads() {
        println("port $port")

        runBlocking {
            accountRepo.findAll().map {
                println(it)
            }.awaitLast()
        }
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
        val result = webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest))
            .exchange()
            .expectStatus().isCreated
            .returnResult(InvoiceResponseDTO::class.java)
            .responseBody

        runBlocking {
            val responseDTO = result.awaitSingle()
            println(responseDTO)

            // прочитаем его из базы. он там должен быть
            val invoice = invoiceRepo.findById(responseDTO.id, dslContext).awaitSingleOrNull()
            assertNotNull(invoice)
            assertEquals(invoice?.customerId, "some_customer_id")
        }

        // создаем еще один такой же - нарушается уникальность, запрос должен упасть
        webClient.post()
            .uri("/invoice")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invoiceRequest))
            .exchange()
            .expectStatus().isBadRequest
    }

}
