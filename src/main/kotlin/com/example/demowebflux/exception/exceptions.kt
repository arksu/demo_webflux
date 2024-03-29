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

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalErrorException(
    message: String
) : AppException(message)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class TronErrorException(
    message: String
) : AppException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class InvoiceAlreadyExists(
    @ResponseErrorProperty
    val orderId: String
) : AppException("Invoice with the same orderId $orderId already exists")

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
class ShopNotFoundException(
    @ResponseErrorProperty
    val shopId: UUID
) : AppException("Shop $shopId is not found")


@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvoiceNotFoundOrLockedException(
    @ResponseErrorProperty
    val id: String
) : AppException("Invoice $id is not found")

@ResponseStatus(HttpStatus.NOT_FOUND)
class InvoiceNotFoundException(
    @ResponseErrorProperty
    val id: String
) : AppException("Invoice $id is not found")


@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class NoActualRateException(
    @ResponseErrorProperty
    val pair: String
) : AppException("No actual rate")

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class WebhookException(
    val code: Int,
    val webhookId: Long,
) : AppException("Error response from webhook $webhookId code=$code")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class NoFreeWalletException : AppException("No free wallet now, please try again later")