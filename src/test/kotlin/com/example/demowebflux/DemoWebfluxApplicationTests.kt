package com.example.demowebflux

import com.example.demowebflux.repo.AccountRepo
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoWebfluxApplicationTests(
    @LocalServerPort
    val port: Int,
) {
    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var accountRepo: AccountRepo

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

}
