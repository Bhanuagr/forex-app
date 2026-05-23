package com.paidy.forex.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "forex")
data class ForexProperties(
    val cacheTtlMinutes: Long,
    val refreshIntervalMs: Long,
    val supportedCurrencies: String,
) {
    fun currencyList(): List<String> =
        supportedCurrencies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
