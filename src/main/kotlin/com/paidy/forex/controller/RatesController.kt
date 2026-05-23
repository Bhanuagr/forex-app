package com.paidy.forex.controller

import com.paidy.forex.domain.Either
import com.paidy.forex.domain.Rate
import com.paidy.forex.service.RateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class RateResponse(
    val from: String,
    val to: String,
    val price: String,
    val timestamp: String,
)

@RestController
class RatesController(private val rateService: RateService) {

    @GetMapping("/rates")
    fun getRate(
        @RequestParam from: String,
        @RequestParam to: String,
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