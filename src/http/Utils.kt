package com.geely.gic.hmi.http

import com.geely.gic.hmi.utils.gson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.locations.Location
import io.ktor.locations.locations
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.contentType
import io.ktor.routing.method
import io.ktor.util.toMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger by lazy { LoggerFactory.getLogger("Http") }

/**
 * This this [Route] node, registers [method] route that will change depending on the [ContentType] provided by the client
 * about the content it is going to send.
 *
 * In this case we support several content types serving different content:
 *
 * - [ContentType.MultiPart.FormData]
 * - [ContentType.Application.FormUrlEncoded]
 * - [ContentType.Application.Json]
 * - Others
 */
fun Route.handleRequestWithBodyFor(method: HttpMethod): Unit {
    contentType(ContentType.MultiPart.FormData) {
        method(method) {
            handle {
                val listFiles = call.receive<MultiPartData>().readAllParts().filterIsInstance<PartData.FileItem>()
                call.sendHttpBinResponse {
                    form = call.receive<Parameters>()
                    files = listFiles.associateBy { part -> part.name ?: "a" }
                }
            }
        }
    }
    contentType(ContentType.Application.FormUrlEncoded) {
        method(method) {
            handle {
                call.sendHttpBinResponse {
                    form = call.receive<Parameters>()
                }
            }
        }
    }
    contentType(ContentType.Application.Json) {
        method(method) {
            handle {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val content = call.receive<String>()
                val response = HttpBinResponse(
                    data = content,
                    json = gson.fromJson(content, type),
                    parameters = call.request.queryParameters,
                    headers = call.request.headers.toMap()
                )
                call.respond(response)
            }
        }
    }
    method(method) {
        handle {
            call.sendHttpBinResponse {
                data = call.receive<String>()
            }
        }
    }
}


/**
 * http 重定向
 *
 * Allows to respond with a absolute redirect from a typed [location] instance of a class annotated
 * with [Location] using the Locations feature.
 */
suspend fun ApplicationCall.redirect(location: Any) {
//    val host = request.host() ?: "localhost"
//    val portSpec = request.port().let { if (it == 80) "" else ":$it" }
//    val address = host + portSpec

//    respondRedirect("http://$address${application.locations.href(location)}")
    val result = mapOf("msg" to application.locations.href(location))
    respond(result)
}