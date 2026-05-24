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
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Component
class OneFrameClient(
    private val oneFrameProperties: OneFrameProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val requestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(oneFrameProperties.connectTimeoutMs)
        setReadTimeout(oneFrameProperties.readTimeoutMs)
    }

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(oneFrameProperties.baseUrl)
        .defaultHeader("token", oneFrameProperties.token)
        .requestFactory(requestFactory)
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

            val rates = responses.mapNotNull { it.toDomainOrNull() }
            Either.Right(rates)
        } catch (ex: RestClientException) {
            log.warn("OneFrame HTTP error: ${ex.message}")
            Either.Left(DomainError.RateLookupFailed(ex.message ?: "HTTP error"))
        } catch (ex: Exception) {
            log.warn("OneFrame unexpected error: ${ex.message}")
            Either.Left(DomainError.RateLookupFailed(ex.message ?: "Unexpected error"))
        }
    }

    private fun OneFrameResponse.toDomainOrNull(): Rate? {
        val fromCurrency = Currency.fromString(from).getOrNull()
            ?: run { log.warn("Skipping rate $from->$to — unrecognised currency: $from"); return null }
        val toCurrency = Currency.fromString(to).getOrNull()
            ?: run { log.warn("Skipping rate $from->$to — unrecognised currency: $to"); return null }
        return try {
            Rate(
                pair = RatePair(fromCurrency, toCurrency),
                price = Price(price),
                timestamp = Timestamp(OffsetDateTime.parse(timeStamp)),
            )
        } catch (ex: DateTimeParseException) {
            log.warn("Skipping rate $from->$to — malformed timestamp '$timeStamp': ${ex.message}")
            null
        }
    }
}