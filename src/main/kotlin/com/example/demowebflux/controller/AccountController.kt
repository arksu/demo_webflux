package com.example.demowebflux.controller

import com.example.demowebflux.AccountRepo
import org.springframework.http.MediaType.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/account")
class AccountController(
    val accountRepo: AccountRepo
) {

    @GetMapping("{id}")
    fun getById(@PathVariable id: UUID): Mono<AccountResponseDTO> {
        return accountRepo.findById(id).map {
            AccountResponseDTO(it.id, it.name)
        }
    }
}