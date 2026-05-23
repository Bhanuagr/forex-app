package com.paidy.forex.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oneframe")
data class OneFrameProperties(
    val baseUrl: String,
    val token: String,
)
