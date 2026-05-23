package com.paidy.forex.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "oneframe")
data class OneFrameProperties(
    val baseUrl: String,
    val token: String,
)

@ConfigurationProperties(prefix = "forex")
data class ForexProperties(
    val cacheTtlMinutes: Long,
    val refreshIntervalMs: Long,
    val supportedCurrencies: String,
) {
    fun currencyList(): List<String> =
        supportedCurrencies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}