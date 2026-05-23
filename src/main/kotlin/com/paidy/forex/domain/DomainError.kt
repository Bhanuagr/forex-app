package com.paidy.forex.domain

sealed class DomainError {
    data class CurrencyNotFound(val code: String) : DomainError()
    object SameCurrencyPair : DomainError()
    object RateNotAvailable : DomainError()
    data class RateLookupFailed(val msg: String) : DomainError()
}