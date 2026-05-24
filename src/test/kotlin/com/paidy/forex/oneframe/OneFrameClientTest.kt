package com.paidy.forex.oneframe

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.paidy.forex.config.OneFrameProperties
import com.paidy.forex.domain.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OneFrameClientTest {

    private lateinit var wireMock: WireMockServer
    private lateinit var client: OneFrameClient

    @BeforeEach
    fun setUp() {
        wireMock = WireMockServer(wireMockConfig().dynamicPort())
        wireMock.start()

        client = OneFrameClient(
            OneFrameProperties(
                baseUrl = "http://localhost:${wireMock.port()}",
                token = "test-token",
                connectTimeoutMs = 3000,
                readTimeoutMs = 5000,
            )
        )
    }

    @AfterEach
    fun tearDown() {
        wireMock.stop()
    }

    @Test
    fun `fetchAll returns rates for single pair`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .withQueryParam("pair", equalTo("USDJPY"))
                .withHeader("token", equalTo("test-token"))
                .willReturn(
                    okJson("""
                        [{"from":"USD","to":"JPY","bid":0.61,"ask":0.82,"price":0.71,"time_stamp":"2026-05-23T10:00:00.000Z"}]
                    """.trimIndent())
                )
        )

        val result = client.fetchAll(listOf(RatePair(Currency.USD, Currency.JPY)))

        assertTrue(result is Either.Right)
        val rates = (result as Either.Right).value
        assertEquals(1, rates.size)
        assertEquals(Currency.USD, rates[0].pair.from)
        assertEquals(Currency.JPY, rates[0].pair.to)
        assertEquals("0.71", rates[0].price.value.toPlainString())
    }

    @Test
    fun `fetchAll returns rates for multiple pairs`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(
                    okJson("""
                        [
                          {"from":"USD","to":"JPY","bid":0.61,"ask":0.82,"price":0.71,"time_stamp":"2026-05-23T10:00:00.000Z"},
                          {"from":"EUR","to":"USD","bid":0.55,"ask":0.75,"price":0.65,"time_stamp":"2026-05-23T10:00:00.000Z"}
                        ]
                    """.trimIndent())
                )
        )

        val result = client.fetchAll(listOf(
            RatePair(Currency.USD, Currency.JPY),
            RatePair(Currency.EUR, Currency.USD),
        ))

        assertTrue(result is Either.Right)
        assertEquals(2, (result as Either.Right).value.size)
    }

    @Test
    fun `fetchAll sends correct token header`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .withHeader("token", equalTo("test-token"))
                .willReturn(okJson("[]"))
        )

        client.fetchAll(listOf(RatePair(Currency.USD, Currency.JPY)))

        wireMock.verify(getRequestedFor(urlPathEqualTo("/rates"))
            .withHeader("token", equalTo("test-token")))
    }

    @Test
    fun `fetchAll returns Left on 500 error`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(serverError())
        )

        val result = client.fetchAll(listOf(RatePair(Currency.USD, Currency.JPY)))

        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DomainError.RateLookupFailed)
    }

    @Test
    fun `fetchAll returns Left on 401 unauthorized`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(unauthorized())
        )

        val result = client.fetchAll(listOf(RatePair(Currency.USD, Currency.JPY)))

        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is DomainError.RateLookupFailed)
    }

    @Test
    fun `fetchAll returns Right empty list for empty pairs`() {
        val result = client.fetchAll(emptyList())

        assertTrue(result is Either.Right)
        assertEquals(0, (result as Either.Right).value.size)
        wireMock.verify(0, getRequestedFor(anyUrl()))
    }

    @Test
    fun `fetchAll builds query string with all pairs`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(
                    okJson("""
                        [
                          {"from":"USD","to":"JPY","bid":0.6,"ask":0.8,"price":0.7,"time_stamp":"2026-05-23T10:00:00.000Z"},
                          {"from":"EUR","to":"GBP","bid":0.5,"ask":0.7,"price":0.6,"time_stamp":"2026-05-23T10:00:00.000Z"}
                        ]
                    """.trimIndent())
                )
        )

        client.fetchAll(listOf(
            RatePair(Currency.USD, Currency.JPY),
            RatePair(Currency.EUR, Currency.GBP),
        ))

        wireMock.verify(getRequestedFor(urlPathEqualTo("/rates"))
            .withQueryParam("pair", equalTo("USDJPY")))
        wireMock.verify(getRequestedFor(urlPathEqualTo("/rates"))
            .withQueryParam("pair", equalTo("EURGBP")))
    }

    @Test
    fun `fetchAll returns Left when OneFrame is unreachable`() {
        wireMock.stop()

        val result = client.fetchAll(listOf(RatePair(Currency.USD, Currency.JPY)))

        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is DomainError.RateLookupFailed)

        wireMock.start()
    }

    @Test
    fun `fetchAll skips entries with unrecognised currency and returns valid ones`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(
                    okJson("""
                        [
                          {"from":"USD","to":"XYZ","bid":0.6,"ask":0.8,"price":0.7,"time_stamp":"2026-05-23T10:00:00.000Z"},
                          {"from":"EUR","to":"GBP","bid":0.5,"ask":0.7,"price":0.6,"time_stamp":"2026-05-23T10:00:00.000Z"}
                        ]
                    """.trimIndent())
                )
        )

        val result = client.fetchAll(listOf(
            RatePair(Currency.USD, Currency.JPY),
            RatePair(Currency.EUR, Currency.GBP),
        ))

        assertTrue(result is Either.Right)
        val rates = (result as Either.Right).value
        assertEquals(1, rates.size)
        assertEquals(Currency.EUR, rates[0].pair.from)
        assertEquals(Currency.GBP, rates[0].pair.to)
    }

    @Test
    fun `fetchAll skips entries with malformed timestamp and returns valid ones`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(
                    okJson("""
                        [
                          {"from":"USD","to":"JPY","bid":0.6,"ask":0.8,"price":0.7,"time_stamp":"not-a-date"},
                          {"from":"EUR","to":"GBP","bid":0.5,"ask":0.7,"price":0.6,"time_stamp":"2026-05-23T10:00:00.000Z"}
                        ]
                    """.trimIndent())
                )
        )

        val result = client.fetchAll(listOf(
            RatePair(Currency.USD, Currency.JPY),
            RatePair(Currency.EUR, Currency.GBP),
        ))

        assertTrue(result is Either.Right)
        val rates = (result as Either.Right).value
        assertEquals(1, rates.size)
        assertEquals(Currency.EUR, rates[0].pair.from)
        assertEquals(Currency.GBP, rates[0].pair.to)
    }
}
