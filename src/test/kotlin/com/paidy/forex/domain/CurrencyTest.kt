package com.paidy.forex.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CurrencyTest {

    @Test
    fun `fromString returns Right for valid uppercase code`() {
        val result = Currency.fromString("USD")
        assertTrue(result is Either.Right)
        assertEquals(Currency.USD, (result as Either.Right).value)
    }

    @Test
    fun `fromString is case insensitive`() {
        val result = Currency.fromString("usd")
        assertTrue(result is Either.Right)
        assertEquals(Currency.USD, (result as Either.Right).value)
    }

    @Test
    fun `fromString returns Left for unknown code`() {
        val result = Currency.fromString("XYZ")
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DomainError.CurrencyNotFound)
        assertEquals("XYZ", (error as DomainError.CurrencyNotFound).code)
    }

    @Test
    fun `fromString returns Left for empty string`() {
        val result = Currency.fromString("")
        assertTrue(result is Either.Left)
    }

    @Test
    fun `all supported currencies parse correctly`() {
        listOf("AUD", "CAD", "CHF", "EUR", "GBP", "NZD", "JPY", "SGD", "USD").forEach { code ->
            val result = Currency.fromString(code)
            assertTrue(result is Either.Right, "Expected Right for $code")
        }
    }

    @Test
    fun `toString returns correct code`() {
        assertEquals("USD", Currency.USD.toString())
        assertEquals("JPY", Currency.JPY.toString())
        assertEquals("EUR", Currency.EUR.toString())
    }

    @Test
    fun `all() returns 9 currencies`() {
        assertEquals(9, Currency.all().size)
    }
}
