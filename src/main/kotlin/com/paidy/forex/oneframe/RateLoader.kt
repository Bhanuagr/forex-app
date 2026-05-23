package com.paidy.forex.oneframe

import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

class RateLoadException(msg: String) : RuntimeException(msg)

@Component
class RateLoader(private val oneFrameClient: OneFrameClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Retryable(
        retryFor = [RateLoadException::class],
        maxAttempts = 4,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun load(pairs: List<RatePair>): List<Rate> =
        when (val result = oneFrameClient.fetchAll(pairs)) {
            is Either.Right -> result.value
            is Either.Left -> throw RateLoadException(result.value.toString())
        }

    @Recover
    fun recover(ex: RateLoadException, pairs: List<RatePair>): List<Rate> {
        log.error("All retry attempts exhausted fetching ${pairs.size} pairs: ${ex.message}")
        throw ex
    }
}
