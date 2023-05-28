package com.example.demowebflux.exception

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*

abstract class AppException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class CurrencyNotFoundException(
    @ResponseErrorProperty
    val currency: String
) : AppException("Currency $currency is not found")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class MerchantNotFoundException(
    @ResponseErrorProperty
    val merchantId: UUID
) : AppException("Merchant $merchantId is not found")