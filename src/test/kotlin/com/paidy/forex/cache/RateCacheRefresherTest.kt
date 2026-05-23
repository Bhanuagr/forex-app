package com.paidy.forex.cache

import com.paidy.forex.config.ForexProperties
import com.paidy.forex.domain.*
import com.paidy.forex.oneframe.RateLoadException
import com.paidy.forex.oneframe.RateLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.OffsetDateTime

class RateCacheRefresherTest {

    private val rateLoader: RateLoader = mock()
    private val rateCache: RateCache = mock()

    private val properties = ForexProperties(
        cacheTtlMinutes = 5,
        refreshIntervalMs = 120000,
        supportedCurrencies = "USD,JPY,EUR",
    )

    private val refresher = RateCacheRefresher(rateLoader, rateCache, properties)

    private fun rate(from: Currency, to: Currency) = Rate(
        pair = RatePair(from, to),
        price = Price(BigDecimal("1.0")),
        timestamp = Timestamp(OffsetDateTime.now()),
    )

    @Test
    fun `refresh calls rateLoader with all currency pairs`() {
        whenever(rateLoader.load(any())).thenReturn(emptyList())

        refresher.refresh()

        val captor = argumentCaptor<List<RatePair>>()
        verify(rateLoader).load(captor.capture())
        val pairs = captor.firstValue
        assertEquals(3, pairs.size, "3 currencies = 3 unique pairs, reverse handled by cache")
        assertTrue(pairs.contains(RatePair(Currency.USD, Currency.JPY)))
        assertTrue(pairs.contains(RatePair(Currency.USD, Currency.EUR)))
        assertTrue(pairs.contains(RatePair(Currency.JPY, Currency.EUR)))
    }

    @Test
    fun `refresh puts rates in cache when load succeeds`() {
        val rates = listOf(rate(Currency.USD, Currency.JPY), rate(Currency.JPY, Currency.USD))
        whenever(rateLoader.load(any())).thenReturn(rates)

        refresher.refresh()

        verify(rateCache).putAll(rates)
    }

    @Test
    fun `refresh does not update cache when load throws RateLoadException`() {
        whenever(rateLoader.load(any())).thenThrow(RateLoadException("timeout"))

        refresher.refresh()

        verify(rateCache, never()).putAll(any())
    }

    @Test
    fun `refresh skips unknown currency codes in config`() {
        val badProperties = ForexProperties(
            cacheTtlMinutes = 5,
            refreshIntervalMs = 120000,
            supportedCurrencies = "USD,INVALID,JPY",
        )
        val refresherWithBadConfig = RateCacheRefresher(rateLoader, rateCache, badProperties)
        whenever(rateLoader.load(any())).thenReturn(emptyList())

        refresherWithBadConfig.refresh()

        val captor = argumentCaptor<List<RatePair>>()
        verify(rateLoader).load(captor.capture())
        assertEquals(1, captor.firstValue.size, "Only 1 unique pair for USD and JPY")
    }

    @Test
    fun `refresh handles empty currency config`() {
        val emptyProperties = ForexProperties(
            cacheTtlMinutes = 5,
            refreshIntervalMs = 120000,
            supportedCurrencies = "",
        )
        val refresherEmpty = RateCacheRefresher(rateLoader, rateCache, emptyProperties)
        whenever(rateLoader.load(emptyList())).thenReturn(emptyList())

        refresherEmpty.refresh()

        val captor = argumentCaptor<List<RatePair>>()
        verify(rateLoader).load(captor.capture())
        assertTrue(captor.firstValue.isEmpty())
    }
}
