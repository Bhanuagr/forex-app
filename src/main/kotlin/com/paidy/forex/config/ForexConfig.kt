package com.paidy.forex.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

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

@Configuration
class CacheConfig {

    @Bean
    fun ratePairCache(forexProperties: ForexProperties): Cache<RatePair, Rate> =
        Caffeine.newBuilder()
            .expireAfterWrite(forexProperties.cacheTtlMinutes, TimeUnit.MINUTES)
            .build()
}
