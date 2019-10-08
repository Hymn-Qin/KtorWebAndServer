package com.geely.gic.hmi

import io.ktor.application.*
import io.ktor.features.origin
import io.ktor.request.document
import io.ktor.request.httpMethod
import io.ktor.request.uri
import kotlinx.coroutines.time.delay
import java.time.Duration


fun Application.intercept() {

//    delaying()
    log()
}

fun Application.log() {
    //将拦截器安装到管道中是改变ApplicationCall处理的主要方法。
    //几乎所有的ktor功能都是拦截器，它们在应用程序调用处理的不同阶段执行各种操作。
    //ApplicationCallPipeline.Call 拦截在call阶段
    intercept(ApplicationCallPipeline.Monitoring) {
        //val call: ApplicationCall = request.call
        //val pipeline: ApplicationReceivePipeline = request.pipeline
        //URL, method, scheme, protocol, host, path, httpVersion, remoteHost, clientIp
        //
        //val version: String = request.httpVersion // "HTTP/1.1"
        //val httpMethod: HttpMethod = request.httpMethod // GET, POST...
        //val uri: String = request.uri // Short cut for `origin.uri`
        //val scheme: String = request.origin.scheme // "http" or "https"
        //val host: String? = request.host() // The host part without the port
        //val port: Int = request.port() // Port of request
        //val path: String = request.path() // The uri without the query string
        //val document: String = request.document() // The last component after '/' of the uri
        //val remoteHost: String = request.origin.remoteHost // The IP address of the client doing the request
        //
        val msg = call.request.run { "method:${httpMethod.value}  document:${document()}  uri:$uri" }
        application.log.info(msg)
    }
}

/**
 * This module [Application.intercept]s the infrastructure pipeline adding a step where
 * it asynchronously suspends the execution for a second. Effectively delaying every single request.
 */
fun Application.delaying() {
    intercept(ApplicationCallPipeline.Features) {
        delay(Duration.ofSeconds(1L))
    }
}
