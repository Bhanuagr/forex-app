package com.paidy.forex.oneframe

import com.paidy.forex.config.OneFrameProperties
import com.paidy.forex.domain.Currency
import com.paidy.forex.domain.DomainError
import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Price
import com.paidy.forex.domain.Rate
import com.paidy.forex.domain.RatePair
import com.paidy.forex.domain.Timestamp
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.OffsetDateTime

@Component
class OneFrameClient(
    private val oneFrameProperties: OneFrameProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(oneFrameProperties.baseUrl)
        .defaultHeader("token", oneFrameProperties.token)
        .build()

    fun fetchAll(pairs: List<RatePair>): Either<DomainError, List<Rate>> {
        if (pairs.isEmpty()) return Either.Right(emptyList())

        val query = pairs.joinToString("&") { "pair=${it.from}${it.to}" }

        return try {
            val responses = restClient.get()
                .uri("/rates?$query")
                .retrieve()
                .body(Array<OneFrameResponse>::class.java)

            if (responses == null) {
                log.warn("OneFrame returned null body")
                return Either.Left(DomainError.RateLookupFailed("Empty response from OneFrame"))
            }

            val rates = responses.map { it.toDomain() }
            Either.Right(rates)
        } catch (ex: RestClientException) {
            log.warn("OneFrame HTTP error: ${ex.message}")
            Either.Left(DomainError.RateLookupFailed(ex.message ?: "HTTP error"))
        } catch (ex: Exception) {
            log.warn("OneFrame unexpected error: ${ex.message}")
            Either.Left(DomainError.RateLookupFailed(ex.message ?: "Unexpected error"))
        }
    }

    private fun OneFrameResponse.toDomain(): Rate {
        val fromCurrency = Currency.fromString(from).getOrThrow()
        val toCurrency = Currency.fromString(to).getOrThrow()
        return Rate(
            pair = RatePair(fromCurrency, toCurrency),
            price = Price(price),
            timestamp = Timestamp(OffsetDateTime.parse(timeStamp)),
        )
    }

    private fun <R> Either<DomainError, R>.getOrThrow(): R = when (this) {
        is Either.Right -> value
        is Either.Left -> throw IllegalStateException("Unexpected currency from OneFrame: $value")
    }
}