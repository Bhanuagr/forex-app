package com.paidy.forex.service

import com.paidy.forex.cache.RateCache
import com.paidy.forex.domain.Currency
import com.paidy.forex.domain.DomainError
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.springframework.stereotype.Service

@Service
class RateService(private val rateCache: RateCache) {

    fun getRate(from: String, to: String): Either<DomainError, Rate> {
        val fromCurrency = Currency.fromString(from)
        if (fromCurrency is Either.Left) return fromCurrency

        val toCurrency = Currency.fromString(to)
        if (toCurrency is Either.Left) return toCurrency

        val fromVal = (fromCurrency as Either.Right).value
        val toVal = (toCurrency as Either.Right).value

        if (fromVal == toVal) return Either.Left(DomainError.SameCurrencyPair)

        return rateCache.get(RatePair(fromVal, toVal))
    }
}