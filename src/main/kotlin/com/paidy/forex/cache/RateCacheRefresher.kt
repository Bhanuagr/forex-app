package com.paidy.forex.cache

import com.paidy.forex.config.ForexProperties
import com.paidy.forex.domain.Currency
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.RatePair
import com.paidy.forex.oneframe.RateLoadException
import com.paidy.forex.oneframe.RateLoader
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RateCacheRefresher(
    private val rateLoader: RateLoader,
    private val rateCache: RateCache,
    private val forexProperties: ForexProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${forex.refresh-interval-ms}", initialDelay = 0)
    fun refresh() {
        val pairs = buildPairs()
        log.info("Refreshing ${pairs.size} currency pairs from OneFrame")

        try {
            val rates = rateLoader.load(pairs)
            rateCache.putAll(rates)
            log.info("Cache refreshed with ${rates.size} rates")
        } catch (ex: RateLoadException) {
            log.error("Cache refresh failed after all retry attempts — existing entries will be served until they expire")
        }
    }

    private fun buildPairs(): List<RatePair> =
        forexProperties.currencyList()
            .mapNotNull { code ->
                when (val result = Currency.fromString(code)) {
                    is Either.Right -> result.value
                    is Either.Left -> { log.warn("Unsupported currency in config: $code"); null }
                }
            }
            .let { currencies ->
                currencies.flatMap { from ->
                    currencies.filter { it != from }.map { to -> RatePair(from, to) }
                }
            }
}