package com.paidy.forex

import com.paidy.forex.config.ForexProperties
import com.paidy.forex.config.OneFrameProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableConfigurationProperties(OneFrameProperties::class, ForexProperties::class)
class ForexApplication

fun main(args: Array<String>) {
    runApplication<ForexApplication>(*args)
}
