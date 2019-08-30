package com.geely.gic.hmi.utils

import com.google.gson.reflect.TypeToken
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.contentType
import io.ktor.routing.method
import io.ktor.util.toMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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


