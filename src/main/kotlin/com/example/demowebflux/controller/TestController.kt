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
@RequestMapping("/test")
class TestController(
    val accountRepo: AccountRepo
) {

    @GetMapping("{id}")
    fun test(@PathVariable id: UUID): Mono<AccountResponseDTO> {
        return accountRepo.findById(id).map {
            AccountResponseDTO(it.id, it.name)
        }
    }
}