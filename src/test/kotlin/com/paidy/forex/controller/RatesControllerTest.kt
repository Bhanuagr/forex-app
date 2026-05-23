package com.paidy.forex.controller

import com.paidy.forex.domain.*
import com.paidy.forex.service.RateService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.time.OffsetDateTime

@WebMvcTest(RatesController::class)
class RatesControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var rateService: RateService

    private fun stubRate(from: Currency, to: Currency, price: String): Rate = Rate(
        pair = RatePair(from, to),
        price = Price(BigDecimal(price)),
        timestamp = Timestamp(OffsetDateTime.parse("2026-05-23T10:00:00+00:00")),
    )

    @Test
    fun `returns 200 with rate for valid pair`() {
        whenever(rateService.getRate("USD", "JPY"))
            .thenReturn(Either.Right(stubRate(Currency.USD, Currency.JPY, "148.50")))

        mockMvc.get("/rates?from=USD&to=JPY")
            .andExpect {
                status { isOk() }
                jsonPath("$.from") { value("USD") }
                jsonPath("$.to") { value("JPY") }
                jsonPath("$.price") { value("148.50") }
                jsonPath("$.timestamp") { exists() }
            }
    }

    @Test
    fun `returns 400 for unsupported currency`() {
        whenever(rateService.getRate("XXX", "JPY"))
            .thenReturn(Either.Left(DomainError.CurrencyNotFound("XXX")))

        mockMvc.get("/rates?from=XXX&to=JPY")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("CURRENCY_NOT_FOUND") }
                jsonPath("$.error") { value("Currency 'XXX' is not supported") }
            }
    }

    @Test
    fun `returns 400 for same currency pair`() {
        whenever(rateService.getRate("USD", "USD"))
            .thenReturn(Either.Left(DomainError.SameCurrencyPair))

        mockMvc.get("/rates?from=USD&to=USD")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("SAME_CURRENCY_PAIR") }
            }
    }

    @Test
    fun `returns 503 when rate not available in cache`() {
        whenever(rateService.getRate("USD", "JPY"))
            .thenReturn(Either.Left(DomainError.RateNotAvailable))

        mockMvc.get("/rates?from=USD&to=JPY")
            .andExpect {
                status { isServiceUnavailable() }
                jsonPath("$.code") { value("RATE_NOT_AVAILABLE") }
            }
    }

    @Test
    fun `returns 400 when from param is missing`() {
        mockMvc.get("/rates?to=JPY")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MISSING_PARAMETER") }
            }
    }

    @Test
    fun `returns 400 when to param is missing`() {
        mockMvc.get("/rates?from=USD")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MISSING_PARAMETER") }
            }
    }

    @Test
    fun `returns 502 when upstream lookup fails`() {
        whenever(rateService.getRate("USD", "JPY"))
            .thenReturn(Either.Left(DomainError.RateLookupFailed("connection refused")))

        mockMvc.get("/rates?from=USD&to=JPY")
            .andExpect {
                status { isBadGateway() }
                jsonPath("$.code") { value("RATE_LOOKUP_FAILED") }
            }
    }
}
