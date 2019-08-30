package com.geely.gic.hmi.utils

import io.ktor.application.ApplicationCall
import io.ktor.locations.Location
import io.ktor.locations.locations
import io.ktor.locations.url
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.response.respondRedirect

/**
 * 获取application的地址，并拼接新的请求  访问外部服务器地址
 */
suspend fun ApplicationCall.address(location: Any): String {

    val appConfig = application.environment.config.config("app")
    val apiConfig = appConfig.config("api")
    val host = apiConfig.property("ip").getString()
    val port = apiConfig.property("port").getString()

//    val host = request.host() ?: "localhost"
    val portSpec = port.toInt().let { if (it == 80) "" else ":$it" }//request.port().let { if (it == 80) "" else ":$it" }
    val address = host + portSpec
    return "http://$address${application.locations.href(location)}"
}

suspend fun ApplicationCall.address(): String {

    val appConfig = application.environment.config.config("app")
    val apiConfig = appConfig.config("api")
    val host = apiConfig.property("ip").getString()
    val port = apiConfig.property("port").getString()

//    val host = request.host() ?: "localhost"
    val portSpec = port.toInt().let { if (it == 80) "" else ":$it" }//request.port().let { if (it == 80) "" else ":$it" }
    val address = host + portSpec
    return "http://$address"
}

/**
 * 获取location的请求体
 */
suspend fun ApplicationCall.request(location: Any): String {
    val request = application.locations.href(location)
    val index = request.indexOf('?')
    return request.substring(0, if (index != 0) index else request.length - 1)
}

/**
 * Utility for performing non-permanent redirections using a typed [location] whose class is annotated with [Location].
 */
suspend fun ApplicationCall.respondRedirect(location: Any)  {
//    val host = request.host() ?: "localhost"
//    val portSpec = request.port().let { if (it == 80) "" else ":$it" }
//    val address = host + portSpec
//
//    respondRedirect("http://$address${application.locations.href(location)}")

    respondRedirect(url(location), permanent = false)
}

/**
 * 重定向
 * Allows to respond with a absolute redirect from a typed [location] instance of a class annotated
 * with [Location] using the Locations feature.
 */
suspend fun ApplicationCall.redirect(location: Any) {
//    val host = request.host() ?: "localhost"
//    val portSpec = request.port().let { if (it == 80) "" else ":$it" }
//    val address = host + portSpec
    respondRedirect(location)
//    respondRedirect("http://$address${application.locations.href(location)}")
}