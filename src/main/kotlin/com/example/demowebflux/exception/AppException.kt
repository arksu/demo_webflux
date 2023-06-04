package com.example.demowebflux.exception

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*

abstract class AppException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(
    message: String
) : AppException(message)

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class UnprocessableEntityException(
    message: String
) : AppException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class InvoiceAlreadyExists(
    @ResponseErrorProperty
    val orderId: String
) : AppException("Invoice with the same orderId already exists")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class CurrencyNotFoundException(
    @ResponseErrorProperty
    val currency: String? = null
) : AppException(if (currency != null) "Currency $currency is not found" else "Currency is not found")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class MerchantNotFoundException(
    @ResponseErrorProperty
    val merchantId: UUID
) : AppException("Merchant $merchantId is not found")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvoiceNotFoundOrLockedException(
    @ResponseErrorProperty
    val id: String
) : AppException("Invoice $id is not found")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class NoFreeWalletException : AppException("No free wallet now, please try later")