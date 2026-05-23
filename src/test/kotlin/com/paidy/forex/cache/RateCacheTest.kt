package com.paidy.forex.cache

import com.paidy.forex.config.ForexProperties
import com.paidy.forex.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class RateCacheTest {

    private val properties = ForexProperties(
        cacheTtlMinutes = 5,
        refreshIntervalMs = 240000,
        supportedCurrencies = "USD,JPY,EUR",
    )

    private lateinit var cache: RateCache

    @BeforeEach
    fun setUp() {
        cache = RateCache(properties)
    }

    private fun rate(from: Currency, to: Currency, price: String = "1.23") = Rate(
        pair = RatePair(from, to),
        price = Price(BigDecimal(price)),
        timestamp = Timestamp(OffsetDateTime.now()),
    )

    @Test
    fun `get returns RateNotAvailable when cache is empty`() {
        val result = cache.get(RatePair(Currency.USD, Currency.JPY))
        assertEquals(Either.Left(DomainError.RateNotAvailable), result)
    }

    @Test
    fun `get returns rate after putAll`() {
        val rate = rate(Currency.USD, Currency.JPY, "148.50")
        cache.putAll(listOf(rate))

        val result = cache.get(RatePair(Currency.USD, Currency.JPY))
        assertTrue(result is Either.Right)
        assertEquals(BigDecimal("148.50"), (result as Either.Right).value.price.value)
    }

    @Test
    fun `get returns RateNotAvailable for unknown pair`() {
        cache.putAll(listOf(rate(Currency.USD, Currency.JPY)))
        val result = cache.get(RatePair(Currency.EUR, Currency.GBP))
        assertEquals(Either.Left(DomainError.RateNotAvailable), result)
    }

    @Test
    fun `putAll overwrites existing entry`() {
        cache.putAll(listOf(rate(Currency.USD, Currency.JPY, "100.00")))
        cache.putAll(listOf(rate(Currency.USD, Currency.JPY, "150.00")))

        val result = cache.get(RatePair(Currency.USD, Currency.JPY))
        assertTrue(result is Either.Right)
        assertEquals(BigDecimal("150.00"), (result as Either.Right).value.price.value)
    }

    @Test
    fun `putAll stores multiple rates`() {
        cache.putAll(listOf(
            rate(Currency.USD, Currency.JPY),
            rate(Currency.EUR, Currency.USD),
            rate(Currency.GBP, Currency.CHF),
        ))

        assertTrue(cache.get(RatePair(Currency.USD, Currency.JPY)) is Either.Right)
        assertTrue(cache.get(RatePair(Currency.EUR, Currency.USD)) is Either.Right)
        assertTrue(cache.get(RatePair(Currency.GBP, Currency.CHF)) is Either.Right)
    }

    @Test
    fun `get returns RateNotAvailable when entry is stale`() {
        val staleCache = RateCache(ForexProperties(
            cacheTtlMinutes = 0,
            refreshIntervalMs = 240000,
            supportedCurrencies = "USD,JPY",
        ))
        staleCache.putAll(listOf(rate(Currency.USD, Currency.JPY)))

        Thread.sleep(1100)

        val result = staleCache.get(RatePair(Currency.USD, Currency.JPY))
        assertEquals(Either.Left(DomainError.RateNotAvailable), result)
    }
}
