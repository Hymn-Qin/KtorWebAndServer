package com.geely.gic.hmi.Intercept

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.response.readText
import io.ktor.content.TextContent
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.util.filter
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose

/**
 * It intercepts all the requests, reverse-proxying them to the wikipedia.
 *
 * In the case of HTML it is completely loaded in memory and preprocessed to change URLs to our own local domain.
 * In the case of other files, the file is streamed from the HTTP client to the HTTP server response.
 */

fun Application.intercept(client: HttpClient) {

    val wikipediaLang = "en"
    // Let's intercept all the requests at the [ApplicationCallPipeline.Call] phase.
    intercept(ApplicationCallPipeline.Call) {
        /**
         * 反向代理
         */
        // We create a GET request to the wikipedia domain and return the call (with the request and the unprocessed response).
        val result = client.call("https://$wikipediaLang.wikipedia.org${call.request.uri}")

        // 获取客户端响应的标头
        // Get the relevant headers of the client response.
        val proxiedHeaders = result.response.headers
        val location = proxiedHeaders[HttpHeaders.Location]
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

        // Extension method to process all the served HTML documents
        fun String.stripWikipediaDomain() = this.replace(Regex("(https?:)?//\\w+\\.wikipedia\\.org"), "")

        //
        // 从标头中删除 en 域
        // Propagates location header, removing the wikipedia domain from it
        if (location != null) {
            call.response.header(HttpHeaders.Location, location.stripWikipediaDomain())
        }

        // Depending on the ContentType, we process the request one way or another.
        when {
            // 将请求的网页内容按HTML格式返回
            // In the case of HTML we download the whole content and process it as a string replacing
            // wikipedia links.
            contentType?.startsWith("text/html") == true -> {
                val text = result.response.readText()
                val filteredText = text.stripWikipediaDomain()
                call.respond(
                    TextContent(
                        filteredText,
                        ContentType.Text.Html.withCharset(Charsets.UTF_8),
                        result.response.status
                    )
                )
            }
            else -> {
                // 对于其他内容进行复制返回
                // In the case of other content, we simply pipe it. We return a [OutgoingContent.WriteChannelContent]
                // propagating the contentLength, the contentType and other headers, and simply we copy
                // the ByteReadChannel from the HTTP client response, to the HTTP server ByteWriteChannel response.
                call.respond(object : OutgoingContent.WriteChannelContent() {
                    override val contentLength: Long? = contentLength?.toLong()
                    override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                    override val headers: Headers = Headers.build {
                        appendAll(proxiedHeaders.filter { key, _ -> !key.equals(HttpHeaders.ContentType, ignoreCase = true) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true) })
                    }
                    override val status: HttpStatusCode? = result.response.status
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        result.response.content.copyAndClose(channel)
                    }
                })
            }
        }
    }
}