package com.paidy.forex

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class ForexIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var rateCacheRefresher: com.paidy.forex.cache.RateCacheRefresher

    companion object {
        private lateinit var wireMock: WireMockServer

        private val oneFrameResponse: String =
            ForexIntegrationTest::class.java.getResource("/oneframe-rates.json")!!.readText()

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMock = WireMockServer(wireMockConfig().dynamicPort())
            wireMock.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMock.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("oneframe.base-url") { "http://localhost:${wireMock.port()}" }
            registry.add("forex.refresh-interval-ms") { "3600000" }
        }
    }

    @BeforeEach
    fun stubOneFrame() {
        wireMock.resetAll()
        wireMock.stubFor(
            get(urlPathEqualTo("/rates"))
                .willReturn(okJson(oneFrameResponse))
        )
    }

    @Test
    fun `returns USD to JPY rate from cache`() {
        triggerCacheRefresh()

        mockMvc.get("/rates?from=USD&to=JPY")
            .andExpect {
                status { isOk() }
                jsonPath("$.from") { value("USD") }
                jsonPath("$.to") { value("JPY") }
                jsonPath("$.price") { value("0.71") }
                jsonPath("$.timestamp") { exists() }
            }
    }

    @Test
    fun `returns inverted JPY to USD rate from cache`() {
        triggerCacheRefresh()

        mockMvc.get("/rates?from=JPY&to=USD")
            .andExpect {
                status { isOk() }
                jsonPath("$.from") { value("JPY") }
                jsonPath("$.to") { value("USD") }
                jsonPath("$.price") { exists() }
            }
    }

    @Test
    fun `returns EUR to GBP rate from cache`() {
        triggerCacheRefresh()

        mockMvc.get("/rates?from=EUR&to=GBP")
            .andExpect {
                status { isOk() }
                jsonPath("$.from") { value("EUR") }
                jsonPath("$.to") { value("GBP") }
                jsonPath("$.price") { value("0.57") }
            }
    }

    @Test
    fun `returns 400 for unsupported currency end to end`() {
        mockMvc.get("/rates?from=BTC&to=USD")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("CURRENCY_NOT_FOUND") }
            }
    }

    @Test
    fun `returns 400 for same currency pair end to end`() {
        mockMvc.get("/rates?from=USD&to=USD")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("SAME_CURRENCY_PAIR") }
            }
    }

    @Test
    fun `returns 400 when from param missing`() {
        mockMvc.get("/rates?to=JPY")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MISSING_PARAMETER") }
            }
    }

    @Test
    fun `returns 400 when to param missing`() {
        mockMvc.get("/rates?from=USD")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MISSING_PARAMETER") }
            }
    }

    @Test
    fun `returns 503 when OneFrame is down and cache is empty`() {
        wireMock.resetAll()
        wireMock.stubFor(get(urlPathEqualTo("/rates")).willReturn(serverError()))

        mockMvc.get("/rates?from=USD&to=JPY")
            .andExpect {
                status { isServiceUnavailable() }
                jsonPath("$.code") { value("RATE_NOT_AVAILABLE") }
            }
    }

    @Test
    fun `cache serves multiple requests without hitting OneFrame each time`() {
        triggerCacheRefresh()
        wireMock.resetAll()

        repeat(5) {
            mockMvc.get("/rates?from=USD&to=JPY")
                .andExpect { status { isOk() } }
        }

        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/rates")))
    }

    private fun triggerCacheRefresh() {
        rateCacheRefresher.refresh()
    }
}
