package com.paidy.forex.domain

data class Rate(
    val pair: RatePair,
    val price: Price,
    val timestamp: Timestamp,
)