package com.geely.gic.hmi.web

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import kotlinx.coroutines.time.delay
import java.time.Duration

/**
 * This module [Application.intercept]s the infrastructure pipeline adding a step where
 * it asynchronously suspends the execution for a second. Effectively delaying every single request.
 */
fun Application.module() {

    intercept(ApplicationCallPipeline.Features) {
        delay(Duration.ofSeconds(1L))
    }

}