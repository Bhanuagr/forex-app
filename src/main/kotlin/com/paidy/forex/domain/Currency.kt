package com.paidy.forex.domain

sealed class Currency {
    object AUD : Currency()
    object CAD : Currency()
    object CHF : Currency()
    object EUR : Currency()
    object GBP : Currency()
    object NZD : Currency()
    object JPY : Currency()
    object SGD : Currency()
    object USD : Currency()

    override fun toString(): String = when (this) {
        is AUD -> "AUD"
        is CAD -> "CAD"
        is CHF -> "CHF"
        is EUR -> "EUR"
        is GBP -> "GBP"
        is NZD -> "NZD"
        is JPY -> "JPY"
        is SGD -> "SGD"
        is USD -> "USD"
    }

    companion object {
        private val all: List<Currency> = listOf(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)

        fun all(): List<Currency> = all

        fun fromString(code: String): Either<DomainError, Currency> =
            all.firstOrNull { it.toString().equals(code, ignoreCase = true) }
                ?.let { Either.Right(it) }
                ?: Either.Left(DomainError.CurrencyNotFound(code))
    }
}