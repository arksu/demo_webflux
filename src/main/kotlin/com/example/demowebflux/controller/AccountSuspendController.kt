package com.example.demowebflux.controller

import com.example.demowebflux.service.AccountService
import org.springframework.http.MediaType.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/account_suspend")
class AccountSuspendController(
    val accountService: AccountService
) {

    @GetMapping("{id}")
    suspend fun getById(@PathVariable id: UUID): AccountResponseDTO {
        return accountService.getSuspend(id)
    }
}