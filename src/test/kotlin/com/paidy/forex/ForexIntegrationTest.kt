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

        private val oneFrameResponse = """
            [
              {"from":"USD","to":"JPY","bid":0.61,"ask":0.82,"price":0.71,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"EUR","bid":0.50,"ask":0.70,"price":0.60,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"GBP","bid":0.45,"ask":0.65,"price":0.55,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"AUD","bid":0.70,"ask":0.90,"price":0.80,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"CAD","bid":0.72,"ask":0.92,"price":0.82,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"CHF","bid":0.68,"ask":0.88,"price":0.78,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"NZD","bid":0.74,"ask":0.94,"price":0.84,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"USD","to":"SGD","bid":0.76,"ask":0.96,"price":0.86,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"USD","bid":0.62,"ask":0.83,"price":0.72,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"EUR","bid":0.51,"ask":0.71,"price":0.61,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"GBP","bid":0.46,"ask":0.66,"price":0.56,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"AUD","bid":0.71,"ask":0.91,"price":0.81,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"CAD","bid":0.73,"ask":0.93,"price":0.83,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"CHF","bid":0.69,"ask":0.89,"price":0.79,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"NZD","bid":0.75,"ask":0.95,"price":0.85,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"JPY","to":"SGD","bid":0.77,"ask":0.97,"price":0.87,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"USD","bid":0.60,"ask":0.80,"price":0.70,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"JPY","bid":0.52,"ask":0.72,"price":0.62,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"GBP","bid":0.47,"ask":0.67,"price":0.57,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"AUD","bid":0.72,"ask":0.92,"price":0.82,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"CAD","bid":0.74,"ask":0.94,"price":0.84,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"CHF","bid":0.70,"ask":0.90,"price":0.80,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"NZD","bid":0.76,"ask":0.96,"price":0.86,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"EUR","to":"SGD","bid":0.78,"ask":0.98,"price":0.88,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"USD","bid":0.63,"ask":0.84,"price":0.73,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"JPY","bid":0.53,"ask":0.73,"price":0.63,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"EUR","bid":0.48,"ask":0.68,"price":0.58,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"AUD","bid":0.73,"ask":0.93,"price":0.83,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"CAD","bid":0.75,"ask":0.95,"price":0.85,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"CHF","bid":0.71,"ask":0.91,"price":0.81,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"NZD","bid":0.77,"ask":0.97,"price":0.87,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"GBP","to":"SGD","bid":0.79,"ask":0.99,"price":0.89,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"USD","bid":0.64,"ask":0.85,"price":0.74,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"JPY","bid":0.54,"ask":0.74,"price":0.64,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"EUR","bid":0.49,"ask":0.69,"price":0.59,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"GBP","bid":0.43,"ask":0.63,"price":0.53,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"CAD","bid":0.76,"ask":0.96,"price":0.86,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"CHF","bid":0.72,"ask":0.92,"price":0.82,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"NZD","bid":0.78,"ask":0.98,"price":0.88,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"AUD","to":"SGD","bid":0.80,"ask":1.00,"price":0.90,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"USD","bid":0.65,"ask":0.86,"price":0.75,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"JPY","bid":0.55,"ask":0.75,"price":0.65,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"EUR","bid":0.50,"ask":0.70,"price":0.60,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"GBP","bid":0.44,"ask":0.64,"price":0.54,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"AUD","bid":0.77,"ask":0.97,"price":0.87,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"CHF","bid":0.73,"ask":0.93,"price":0.83,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"NZD","bid":0.79,"ask":0.99,"price":0.89,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CAD","to":"SGD","bid":0.81,"ask":1.00,"price":0.91,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"USD","bid":0.66,"ask":0.87,"price":0.76,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"JPY","bid":0.56,"ask":0.76,"price":0.66,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"EUR","bid":0.51,"ask":0.71,"price":0.61,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"GBP","bid":0.45,"ask":0.65,"price":0.55,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"AUD","bid":0.78,"ask":0.98,"price":0.88,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"CAD","bid":0.74,"ask":0.94,"price":0.84,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"NZD","bid":0.80,"ask":1.00,"price":0.90,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"CHF","to":"SGD","bid":0.82,"ask":1.00,"price":0.92,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"USD","bid":0.67,"ask":0.88,"price":0.77,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"JPY","bid":0.57,"ask":0.77,"price":0.67,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"EUR","bid":0.52,"ask":0.72,"price":0.62,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"GBP","bid":0.46,"ask":0.66,"price":0.56,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"AUD","bid":0.79,"ask":0.99,"price":0.89,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"CAD","bid":0.75,"ask":0.95,"price":0.85,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"CHF","bid":0.81,"ask":1.00,"price":0.91,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"NZD","to":"SGD","bid":0.83,"ask":1.00,"price":0.93,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"USD","bid":0.68,"ask":0.89,"price":0.78,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"JPY","bid":0.58,"ask":0.78,"price":0.68,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"EUR","bid":0.53,"ask":0.73,"price":0.63,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"GBP","bid":0.47,"ask":0.67,"price":0.57,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"AUD","bid":0.80,"ask":1.00,"price":0.90,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"CAD","bid":0.76,"ask":0.96,"price":0.86,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"CHF","bid":0.82,"ask":1.00,"price":0.92,"time_stamp":"2026-05-23T10:00:00.000Z"},
              {"from":"SGD","to":"NZD","bid":0.84,"ask":1.00,"price":0.94,"time_stamp":"2026-05-23T10:00:00.000Z"}
            ]
        """.trimIndent()

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
    fun `returns EUR to USD rate from cache`() {
        triggerCacheRefresh()

        mockMvc.get("/rates?from=EUR&to=USD")
            .andExpect {
                status { isOk() }
                jsonPath("$.from") { value("EUR") }
                jsonPath("$.to") { value("USD") }
                jsonPath("$.price") { value("0.70") }
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
