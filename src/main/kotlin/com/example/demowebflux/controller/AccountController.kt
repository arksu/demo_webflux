package com.example.demowebflux.controller

import com.example.demowebflux.repo.AccountRepo
import com.example.demowebflux.service.AccountService
import com.example.jooq.Tables
import com.example.jooq.tables.pojos.Account
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.http.MediaType.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/account")
class AccountController(
    val accountService: AccountService,
) {

    @GetMapping
    fun getAll(): Flux<AccountResponseDTO> {
        return accountService.getAllFlux()
    }

    @GetMapping("{id}")
    fun getById(@PathVariable id: UUID): Mono<AccountResponseDTO> {
        return accountService.getMono(id)
    }

    @GetMapping("s/{id}")
    suspend fun getSById(@PathVariable id: UUID): AccountResponseDTO {
        return accountService.getSuspend(id)
    }
}