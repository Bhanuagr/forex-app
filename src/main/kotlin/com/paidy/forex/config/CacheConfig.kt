package com.paidy.forex.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig {

    @Bean
    fun ratePairCache(forexProperties: ForexProperties): Cache<RatePair, Rate> =
        Caffeine.newBuilder()
            .expireAfterWrite(forexProperties.cacheTtlMinutes, TimeUnit.MINUTES)
            .build()
}
