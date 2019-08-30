package com.geely.gic.hmi.utils

import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.http.isSuccess
import io.ktor.request.receiveMultipart
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.cio.writeChannel
import io.ktor.util.hex
import kotlinx.coroutines.*
import kotlinx.coroutines.io.copyAndClose
import kotlinx.coroutines.io.readRemaining
import kotlinx.io.core.readBytes
import kotlinx.io.errors.IOException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}


fun main(args: Array<String>) {
    runBlocking {
        val client = HttpClient(Apache) {
            followRedirects = true
        }
        client.getAsTempFile("http://127.0.0.1:8087/") { file ->
            println(file.readBytes().size)
        }
    }

//    embeddedServer(Netty, port = 8080) {
//        module()
//    }.start(true)
}

data class HttpClientException(val response: HttpResponse) : IOException("HTTP Error ${response.status}")

suspend fun HttpClient.getAsTempFile(url: String, callback: suspend (file: File) -> Unit) {
    val file = getAsTempFile(url)
    try {
        callback(file)
    } finally {
        file.delete()
    }
}

suspend fun HttpClient.getAsTempFile(url: String): File {
    val file = File.createTempFile("ktor", "http-client")
    val call = call {
        url(URL(url))
        method = HttpMethod.Get
    }
    if (!call.response.status.isSuccess()) {
        throw HttpClientException(call.response)
    }
    call.response.content.copyAndClose(file.writeChannel())
    return file
}

fun main1(args: Array<String>) {
    val client = HttpClient(Apache)

    val port = 8080
    embeddedServer(Netty, port = port) {
        routing {
            get("/") {
                val result = client.post<HttpResponse>("http://127.0.0.1:$port/handler") {
                    body = MultiPartContent.build {
                        add("user", "myuser")
                        add("password", "password")
                        add("file", byteArrayOf(1, 2, 3, 4), filename = "binary.bin")
                    }
                }
                call.respondText(result.content.readRemaining().readText())
            }
            post("/handler") {
                val multipart = call.receiveMultipart()
                val out = arrayListOf<String>()
                multipart.forEachPart { part ->
                    out += when (part) {
                        is PartData.FormItem -> {
                            "FormItem(${part.name},${part.value})"
                        }
                        is PartData.FileItem -> {
                            val bytes = part.streamProvider().readBytes()
                            "FileItem(${part.name},${part.originalFileName},${hex(bytes)})"
                        }
                        is PartData.BinaryItem -> {
                            "BinaryItem(${part.name},${hex(part.provider().readBytes())})"
                        }
                    }

                    part.dispose()
                }
                call.respondText(out.joinToString("; "))
            }
        }
    }.start(true)
}

