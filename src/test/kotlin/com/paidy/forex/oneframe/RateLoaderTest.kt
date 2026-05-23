package com.paidy.forex.oneframe

import com.paidy.forex.ForexApplication
import com.paidy.forex.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.math.BigDecimal
import java.time.OffsetDateTime

@SpringBootTest(classes = [ForexApplication::class])
class RateLoaderTest {

    @Autowired
    lateinit var rateLoader: RateLoader

    @MockBean
    lateinit var oneFrameClient: OneFrameClient

    private val pairs = listOf(RatePair(Currency.USD, Currency.JPY))

    private val rate = Rate(
        pair = RatePair(Currency.USD, Currency.JPY),
        price = Price(BigDecimal("0.71")),
        timestamp = Timestamp(OffsetDateTime.now()),
    )

    @Test
    fun `load returns rates on first attempt`() {
        whenever(oneFrameClient.fetchAll(pairs)).thenReturn(Either.Right(listOf(rate)))

        val result = rateLoader.load(pairs)

        assertEquals(1, result.size)
        verify(oneFrameClient, times(1)).fetchAll(pairs)
    }

    @Test
    fun `load retries on failure and succeeds on second attempt`() {
        whenever(oneFrameClient.fetchAll(pairs))
            .thenReturn(Either.Left(DomainError.RateLookupFailed("timeout")))
            .thenReturn(Either.Right(listOf(rate)))

        val result = rateLoader.load(pairs)

        assertEquals(1, result.size)
        verify(oneFrameClient, times(2)).fetchAll(pairs)
    }

    @Test
    fun `load retries up to 4 attempts then throws RateLoadException`() {
        whenever(oneFrameClient.fetchAll(pairs))
            .thenReturn(Either.Left(DomainError.RateLookupFailed("timeout")))

        assertThrows(RateLoadException::class.java) {
            rateLoader.load(pairs)
        }

        verify(oneFrameClient, times(4)).fetchAll(pairs)
    }

    @Test
    fun `load succeeds on final allowed attempt`() {
        whenever(oneFrameClient.fetchAll(pairs))
            .thenReturn(Either.Left(DomainError.RateLookupFailed("timeout")))
            .thenReturn(Either.Left(DomainError.RateLookupFailed("timeout")))
            .thenReturn(Either.Left(DomainError.RateLookupFailed("timeout")))
            .thenReturn(Either.Right(listOf(rate)))

        val result = rateLoader.load(pairs)

        assertEquals(1, result.size)
        verify(oneFrameClient, times(4)).fetchAll(pairs)
    }
}
