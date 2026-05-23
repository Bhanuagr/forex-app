package com.paidy.forex.cache

import com.github.benmanes.caffeine.cache.Cache
import com.paidy.forex.domain.DomainError
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.springframework.stereotype.Component

@Component
class RateCache(private val store: Cache<RatePair, Rate>) {

    fun get(pair: RatePair): Either<DomainError, Rate> =
        store.getIfPresent(pair)
            ?.let { Either.Right(it) }
            ?: Either.Left(DomainError.RateNotAvailable)

    fun putAll(rates: List<Rate>) =
        rates.forEach { store.put(it.pair, it) }
}
