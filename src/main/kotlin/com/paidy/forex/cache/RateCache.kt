package com.paidy.forex.cache

import com.paidy.forex.config.ForexProperties
import com.paidy.forex.domain.DomainError
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class RateCache(private val forexProperties: ForexProperties) {

    private data class CachedEntry(val rate: Rate, val fetchedAt: Instant)

    private val store = ConcurrentHashMap<RatePair, CachedEntry>()

    fun get(pair: RatePair): Either<DomainError, Rate> {
        val entry = store[pair] ?: return Either.Left(DomainError.RateNotAvailable)
        val ttl = forexProperties.cacheTtlMinutes * 60
        val age = Instant.now().epochSecond - entry.fetchedAt.epochSecond
        return if (age > ttl) {
            Either.Left(DomainError.RateNotAvailable)
        } else {
            Either.Right(entry.rate)
        }
    }

    fun putAll(rates: List<Rate>) {
        val now = Instant.now()
        rates.forEach { rate ->
            store[rate.pair] = CachedEntry(rate, now)
        }
    }
}