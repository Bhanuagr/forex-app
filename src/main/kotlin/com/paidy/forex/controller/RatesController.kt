package com.paidy.forex.controller

import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Rate
import com.paidy.forex.service.RateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class RateResponse(
    @Schema(description = "Source currency code", example = "USD")
    val from: String,
    @Schema(description = "Target currency code", example = "JPY")
    val to: String,
    @Schema(description = "Mid-market exchange rate", example = "0.71")
    val price: String,
    @Schema(description = "Timestamp of the rate from One-Frame", example = "2026-05-23T10:00:00Z")
    val timestamp: String,
)

@RestController
@Tag(name = "Rates", description = "Currency exchange rate lookup")
class RatesController(private val rateService: RateService) {

    @Operation(
        summary = "Get exchange rate",
        description = "Returns the mid-market exchange rate between two supported currencies. Rates are cached and refreshed every 2 minutes from One-Frame.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Exchange rate found",
            content = [Content(schema = Schema(implementation = RateResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request - unsupported currency, same currency pair, or missing parameter",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description = "Rate not available - cache is empty or all entries have expired",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "502",
            description = "Upstream One-Frame service failed to respond after retries",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @GetMapping("/rates")
    fun getRate(
        @RequestParam @Schema(description = "Source currency code", example = "USD") from: String,
        @RequestParam @Schema(description = "Target currency code", example = "JPY") to: String,
    ): ResponseEntity<Any> {
        return when (val result = rateService.getRate(from, to)) {
            is Either.Right -> ResponseEntity.ok(result.value.toResponse())
            is Either.Left -> throw DomainException(result.value)
        }
    }

    private fun Rate.toResponse() = RateResponse(
        from = pair.from.toString(),
        to = pair.to.toString(),
        price = price.value.toPlainString(),
        timestamp = timestamp.value.toString(),
    )
}
