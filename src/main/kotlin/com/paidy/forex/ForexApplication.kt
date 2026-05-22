package com.paidy.forex

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ForexApplication

fun main(args: Array<String>) {
    runApplication<ForexApplication>(*args)
}
