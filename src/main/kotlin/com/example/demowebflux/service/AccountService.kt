package com.example.demowebflux.service

import com.example.demowebflux.repo.AccountRepo
import com.example.demowebflux.controller.AccountResponseDTO
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class AccountService(
    private val accountRepo: AccountRepo
) {
    fun getMono(id: UUID): Mono<AccountResponseDTO> {
        return accountRepo.findById(id).map {
            AccountResponseDTO(it.id, it.name)
        }
    }

    suspend fun getSuspend(id: UUID) : AccountResponseDTO {
        return accountRepo.findById(id).awaitSingle().let {
            AccountResponseDTO(it.id, it.name)
        }
    }
}