package com.example.demowebflux.service

import com.example.demowebflux.repo.AccountRepo
import com.example.demowebflux.controller.AccountResponseDTO
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class AccountService(
    private val accountRepo: AccountRepo
) {
    fun get(id: UUID): Mono<AccountResponseDTO> {
        return accountRepo.findById(id).map {
            AccountResponseDTO(it.id, it.name)
        }
    }
}