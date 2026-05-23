package com.paidy.forex.service

import com.paidy.forex.cache.RateCache
import com.paidy.forex.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.OffsetDateTime

class RateServiceTest {

    private val rateCache: RateCache = mock()
    private val service = RateService(rateCache)

    private fun stubRate(from: Currency, to: Currency): Rate = Rate(
        pair = RatePair(from, to),
        price = Price(BigDecimal("1.50")),
        timestamp = Timestamp(OffsetDateTime.now()),
    )

    @Test
    fun `returns rate for valid currency pair`() {
        val rate = stubRate(Currency.USD, Currency.JPY)
        whenever(rateCache.get(RatePair(Currency.USD, Currency.JPY))).thenReturn(Either.Right(rate))

        val result = service.getRate("USD", "JPY")

        assertTrue(result is Either.Right)
        assertEquals(rate, (result as Either.Right).value)
    }

    @Test
    fun `returns CurrencyNotFound for invalid from currency`() {
        val result = service.getRate("XXX", "JPY")
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DomainError.CurrencyNotFound)
        assertEquals("XXX", (error as DomainError.CurrencyNotFound).code)
    }

    @Test
    fun `returns CurrencyNotFound for invalid to currency`() {
        val result = service.getRate("USD", "ZZZ")
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DomainError.CurrencyNotFound)
        assertEquals("ZZZ", (error as DomainError.CurrencyNotFound).code)
    }

    @Test
    fun `returns SameCurrencyPair when from equals to`() {
        val result = service.getRate("USD", "USD")
        assertTrue(result is Either.Left)
        assertEquals(DomainError.SameCurrencyPair, (result as Either.Left).value)
    }

    @Test
    fun `currency codes are case insensitive`() {
        val rate = stubRate(Currency.USD, Currency.JPY)
        whenever(rateCache.get(RatePair(Currency.USD, Currency.JPY))).thenReturn(Either.Right(rate))

        val result = service.getRate("usd", "jpy")

        assertTrue(result is Either.Right)
    }

    @Test
    fun `returns RateNotAvailable when cache misses`() {
        whenever(rateCache.get(RatePair(Currency.USD, Currency.JPY)))
            .thenReturn(Either.Left(DomainError.RateNotAvailable))

        val result = service.getRate("USD", "JPY")

        assertTrue(result is Either.Left)
        assertEquals(DomainError.RateNotAvailable, (result as Either.Left).value)
    }
}
