package com.paidy.forex.oneframe

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OneFrameResponse(
    val from: String,
    val to: String,
    val bid: BigDecimal,
    val ask: BigDecimal,
    val price: BigDecimal,
    @JsonProperty("time_stamp") val timeStamp: String,
)