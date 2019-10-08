package com.geely.gic.hmi.utils

import io.ktor.application.ApplicationCall
import io.ktor.application.log
import io.ktor.features.origin
import io.ktor.locations.locations
import io.ktor.locations.url

/**
 * 获取application的地址，并拼接新的请求  访问外部服务器地址
 */
suspend fun ApplicationCall.address(location: Any): String {

    val appConfig = application.environment.config.config("app")
    val apiConfig = appConfig.config("api")
    val host = apiConfig.property("ip").getString()
    val port = apiConfig.property("port").getString()

    val portSpec = port.toInt().let { if (it == 80) "" else ":$it" }
    val address = host + portSpec
    val uri = "${request.origin.scheme}://$address${uri(location)}"
    application.log.info(url(location))
    return uri
}

suspend fun ApplicationCall.address(): String {

    val appConfig = application.environment.config.config("app")
    val apiConfig = appConfig.config("api")
    val host = apiConfig.property("ip").getString()
    val port = apiConfig.property("port").getString()

//    val host = request.host() ?: "localhost"
    val portSpec =
        port.toInt().let { if (it == 80) "" else ":$it" }//request.port().let { if (it == 80) "" else ":$it" }
    val address = host + portSpec
    return "http://$address"
}

/**
 * 获取location的请求体
 */
suspend fun ApplicationCall.uri(location: Any): String {
    val request = application.locations.href(location)
    val index = request.indexOf('?')
    return request.substring(0, if (index != 0) index else request.length - 1)
}