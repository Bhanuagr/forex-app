package com.paidy.forex.cache

import com.paidy.forex.config.ForexProperties
import com.paidy.forex.domain.Currency
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.RatePair
import com.paidy.forex.oneframe.OneFrameClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RateCacheRefresher(
    private val oneFrameClient: OneFrameClient,
    private val rateCache: RateCache,
    private val forexProperties: ForexProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${forex.refresh-interval-ms}")
    fun refresh() {
        val currencies = forexProperties.currencyList()
            .mapNotNull { code ->
                Currency.fromString(code).let { result ->
                    when (result) {
                        is Either.Right -> result.value
                        is Either.Left -> {
                            log.warn("Unsupported currency in config: $code")
                            null
                        }
                    }
                }
            }

        val pairs = currencies.flatMap { from ->
            currencies.filter { it != from }.map { to -> RatePair(from, to) }
        }

        log.info("Refreshing ${pairs.size} currency pairs from OneFrame")

        when (val result = oneFrameClient.fetchAll(pairs)) {
            is Either.Right -> {
                rateCache.putAll(result.value)
                log.info("Cache refreshed with ${result.value.size} rates")
            }
            is Either.Left -> {
                log.warn("OneFrame refresh failed: ${result.value} — keeping existing cache entries")
            }
        }
    }
}