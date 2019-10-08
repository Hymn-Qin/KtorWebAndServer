package com.geely.gic.hmi

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpSendPipeline
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("HttpClient")

fun HttpClient.intercept(){
    log()
}

fun HttpClient.log() {
    this.sendPipeline.intercept(HttpSendPipeline.Engine) {
        val msg = context.run { "method:${method.value}  url:${this.url.buildString()}  uri:${url.encodedPath}"  }
        logger.info(msg)

    }
}
