package com.paidy.forex.controller

import com.paidy.forex.domain.DomainError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val error: String, val code: String)

@RestControllerAdvice
class ErrorHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<ErrorResponse> {
        val (status, response) = when (val err = ex.error) {
            is DomainError.CurrencyNotFound ->
                HttpStatus.BAD_REQUEST to ErrorResponse(
                    error = "Currency '${err.code}' is not supported",
                    code = "CURRENCY_NOT_FOUND",
                )
            is DomainError.SameCurrencyPair ->
                HttpStatus.BAD_REQUEST to ErrorResponse(
                    error = "from and to currencies must be different",
                    code = "SAME_CURRENCY_PAIR",
                )
            is DomainError.RateNotAvailable ->
                HttpStatus.SERVICE_UNAVAILABLE to ErrorResponse(
                    error = "Rate not available, please try again later",
                    code = "RATE_NOT_AVAILABLE",
                )
            is DomainError.RateLookupFailed ->
                HttpStatus.BAD_GATEWAY to ErrorResponse(
                    error = "Failed to retrieve rate: ${err.msg}",
                    code = "RATE_LOOKUP_FAILED",
                )
        }
        return ResponseEntity.status(status).body(response)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = "Missing required parameter: ${ex.parameterName}",
                code = "MISSING_PARAMETER",
            )
        )
}