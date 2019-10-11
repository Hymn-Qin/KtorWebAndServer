package com.geely.gic.hmi

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestPipeline
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("HttpClient")

fun HttpClient.intercept(){
    log()
}

fun HttpClient.log() {
    this.requestPipeline.intercept(HttpRequestPipeline.Send) {

        val msg = context.run { "method:${method.value}  url:${this.url.buildString()}  uri:${url.encodedPath}"  }
        logger.info(msg)

    }
}
