package com.geely.gic.hmi.http.utils

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.model.Reply
import io.ktor.application.ApplicationCall
import io.ktor.features.callId
import io.ktor.locations.Location
import io.ktor.locations.locations
import io.ktor.locations.url
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.response.respond
import io.ktor.response.respondRedirect

/**
 * 返回错误的响应
 * http 重定向
 *
 * Allows to respond with a absolute redirect from a typed [message] instance of a class annotated
 * with [Location] using the Locations feature.
 */
suspend fun ApplicationCall.redirect(message: Any) {
    val reply = when (message) {
        is Users.Login -> Reply(201, message.error, null)
        is Users.Register -> Reply(202, message.error, null)
        is Users.UserInfo -> Reply(203, message.error, null)
        else -> Reply(200, "other", null)
    }
    logger.info("REPLY: {}", reply)
    respond(reply)
}

/**
 * 返回成功的响应
 */
suspend fun ApplicationCall.respond(result: Any?) {
    val reply = Reply(100, "success", result)
    logger.info("REPLY: {}", reply)
    respond(reply)
}

/**
 * 获取application的地址，并拼接新的请求
 */
suspend fun ApplicationCall.address(location: Any): String {
    val host = request.host() ?: "localhost"
    val portSpec = request.port().let { if (it == 80) "" else ":$it" }
    val address = host + portSpec
    return "http://$address${application.locations.href(location)}"
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