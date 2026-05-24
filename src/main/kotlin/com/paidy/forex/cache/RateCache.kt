package com.paidy.forex.cache

import com.github.benmanes.caffeine.cache.Cache
import com.paidy.forex.domain.DomainError
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Price
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.MathContext

@Component
class RateCache(private val store: Cache<RatePair, Rate>) {

    fun get(pair: RatePair): Either<DomainError, Rate> =
        (store.getIfPresent(pair)?.let { Either.Right(it) }
            ?: store.getIfPresent(pair.reverse())?.let { it.invert(pair) })
            ?: Either.Left(DomainError.RateNotAvailable)

    fun putAll(rates: List<Rate>) =
        rates.forEach { store.put(it.pair, it) }

    private fun RatePair.reverse() = RatePair(to, from)

    private fun Rate.invert(requestedPair: RatePair): Either<DomainError, Rate> {
        if (price.value.compareTo(BigDecimal.ZERO) == 0)
            return Either.Left(DomainError.RateNotAvailable)
        return Either.Right(copy(
            pair = requestedPair,
            price = Price(BigDecimal.ONE.divide(price.value, MathContext.DECIMAL64)),
        ))
    }
}
