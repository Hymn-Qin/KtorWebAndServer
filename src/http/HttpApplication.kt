package com.geely.gic.hmi.http

import com.geely.gic.hmi.data.model.HttpBinError
import com.geely.gic.hmi.data.model.PostSnippet
import com.geely.gic.hmi.data.model.Snippet
import com.geely.gic.hmi.http.utils.*
import com.geely.gic.hmi.http.utils.respond
import com.geely.gic.hmi.utils.gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.gson.GsonConverter
import io.ktor.html.respondHtml
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.http.content.resource
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicLong


/**
 * The entrypoint / main module. Referenced from resources/application.conf#ktor.application.modules
 *
 * More information about the application.conf file here: https://ktor.io/servers/configuration.html#hocon-file
 */

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.httpModule(testing: Boolean = false) {

    //身份认证
//    val simpleJWT = authentication()

    val httpClient = HttpClient(Apache) {
        install(JsonFeature)
    }


    /**
     * Install all the features we are going to use.
     *
     * All the standard available features described here: https://ktor.io/servers/features.html
     */
    install(CallId) {
        header(HttpHeaders.XRequestId)
//        retrieveFromHeader(HttpHeaders.XRequestId)

        val counter = AtomicLong()
        generate { "hltj-me-${counter.incrementAndGet()}" }

        verify { it.isNotEmpty() }
    }

    // 记录所有请求
    // Logs all the requests performed
    install(CallLogging) {
        callIdMdc("request-id")
    }

    // 自动设置日期和标头
    // This feature sets a Date and Server headers automatically.
    install(DefaultHeaders)
    // 自动压缩
    // This feature enables compression automatically when accepted by the client.
    install(Compression)
    // 自动304相应
    // Automatic '304 Not Modified' Responses
    install(ConditionalHeaders)
    // Supports for Range, Accept-Range and Content-Range headers
    install(PartialContent)
    install(Locations)
//    install(FreeMarker) {
//        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
//    }

    // Configure the session to be represented by a [Sessions],
    // using the SESSION cookie to store it, and transforming it to be authenticated with the [hashKey].
    // it is sent in plain text, but since it is authenticated can't be modified without knowing the secret [hashKey].
//    install(Sessions) {
//        cookie<Session>("Session") {
//            transform(SessionTransportTransformerMessageAuthentication(hashKey))
//        }
//    }

    // 检查请求的标头
    // For each GET header, adds an automatic HEAD handler (checks the headers of the requests
    // without actually getting the payload to be more efficient about resources)
    install(AutoHeadResponse)
    // Based on the Accept header, allows to reply with arbitrary objects converting them into JSON
    // when the client accepts it.
    install(ContentNegotiation) {
        register(ContentType.Application.Json, GsonConverter())
//        jackson {
//        }
    }
    // 启用跨资源共享
    // Enables Cross-Origin Resource Sharing (CORS)
    install(CORS) {
        anyHost()
        allowCredentials = true
        listOf(HttpMethod("PATCH"), HttpMethod.Put, HttpMethod.Delete).forEach {
            method(it)
        }
    }
    // 处理没有的请求
    // Here we handle unhandled exceptions from routes
    install(StatusPages) {
        exception<Throwable> { cause ->
            environment.log.error(cause)
            val error = HttpBinError(
                code = HttpStatusCode.InternalServerError,
                request = call.request.local.uri,
                message = cause.toString(),
                cause = cause
            )
            call.respond(error)
        }
    }

    // Fake Authorization with user:password "test:test"
    val hashedUserTable = UserHashedTableAuth(
        getDigestFunction("SHA-256") { "ktor${it.length}" },
        table = mapOf(
            "test" to Base64.getDecoder().decode("GSjkHCHGAxTTbnkEDBbVYd+PUFRlcWiumc4+MWE9Rvw=") // sha256 for "test"
        )
    )


    /**
     * Http 后端服务
     */
    routing {
        //9，Http 内容协商
        val snippets = Collections.synchronizedList(
            mutableListOf(
                Snippet(user = "test", text = "hello"),
                Snippet(user = "test", text = "world")
            )
        )
        //使用 route(path) { } 块将具有相同前缀的路由分组。
        // 对于每个 HTTP 方法，都有一个不带路由路径参数的重载，
        // 可以用于路由的叶子节点
        route("/snippets") {
            get {
                call.respond(mapOf("snippets" to synchronized(snippets) {
                    snippets.toList()
                }))
            }
            //进行身份认证
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    //接收数据{"snippet": {"text" : "mysnippet"}}
//                    snippets += Snippet(post.snippet.text)
                    //接受令牌
                    val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
                    snippets += Snippet(principal.name, post.snippet.text)
                    call.respond(mapOf("OK" to true))
                }
            }

        }

        // Route to test plain 'get' requests.
        // ApplicationCall.sendHttpBinResponse is an extension method defined in this project that sends
        // information about the request as an object, that will be converted into JSON
        // by the ContentNegotiation feature.
        get("/get") {
            call.sendHttpBinResponse()
        }

        // 动态注册路由
        // This is a sample of registering routes "dynamically".
        // We define a map with a pair 'path' to 'method' and then we register it.
        val postPutDelete = mapOf(
            "/post" to HttpMethod.Post,
            "/put" to HttpMethod.Put,
            "/delete" to HttpMethod.Delete,
            "/patch" to HttpMethod("PATCH")
        )
        for ((route, method) in postPutDelete) {
            route(route) {
                // This method will register different handlers for this route ('path' to 'method')
                // depending on the Content-Type provided by the client for the content it is going to send.
                // Since GET or HEAD requests do not have content, it is not applicable for those methods.
                // This handles [ContentType.MultiPart.FormData], [ContentType.Application.FormUrlEncoded],
                // [ContentType.Application.Json] and others.
                handleRequestWithBodyFor(method)
            }
        }

        // 匹配格式
        // Defines an '/image' route that will serve different content, based on the 'Accept' header sent by the client.
        route("/image") {
            val imageConfigs = listOf(
                ImageConfig("jpeg", ContentType.Image.JPEG, "jackal.jpg"),
                ImageConfig("png", ContentType.Image.PNG, "pig_icon.png"),
                ImageConfig("svg", ContentType.Image.SVG, "svg_logo.svg"),
                ImageConfig("webp", ContentType("image", "webp"), "wolf_1.webp"),
                ImageConfig("any", ContentType.Image.Any, "jackal.jpg")
            )
            for ((path, contentType, filename) in imageConfigs) {
                // 当'Accept' header 匹配时返回路径中特定格式的文件
                // Serves this specific file in the specific format in the route when the 'Accept' header makes it the best match.
                // Chrome 收到wolf_1.webp  其他浏览器收到 jackal.jpg
                // So for example a Chrome browser would receive a WEBP image, while another browser like Internet Explorer would receive a JPEG.
                accept(contentType) {
                    resource("", "static/$filename")
                }
                // 除此之外还可以通过 /image/‘format’ 指定格式返回文件 如： /image/any
                // As a fallback, we also serve the file independently on the Accept header, in the `/image/format` route.
                resource(path, "static/$filename")
            }
        }

        // This route sends a response that will include the Headers sent from the client.
        get("/headers") {
            call.sendHttpBinResponse {
                clear()
                headers = call.request.headers.toMap()
            }
        }

        // This route includes the IP of the client. In the case this server is behind a reverse-proxy,
        // you can also register the ForwardedHeaderSupport feature, and the `call.request.origin.remoteHost`
        // would return the user's IP, while `call.request.local.remoteHost` would return the IP of the reverse proxy.
        get("/ip") {
            call.sendHttpBinResponse {
                clear()
                origin = call.request.origin.remoteHost
            }
        }

        // @TODO: Forces a gzipped response?
        get("/gzip") {
            call.sendHttpBinResponse {
                gzipped = true
            }
        }

        // @TODO: Forces a deflated response?
        get("/deflate") {
            // Send header "Accept-Encoding: deflate"
            call.sendHttpBinResponse {
                deflated = true
            }
        }

        // This can be done using the [ConditionalHeaders] feature and setting the
        // ETag and Last-Modified headers to the response content.
        get("/cache") {
            val etag = "db7a0a2684bb439e858ee25ae5b9a5c6"
            val date: ZonedDateTime = ZonedDateTime.of(2016, 2, 15, 0, 0, 0, 0, ZoneId.of("Z")) // Kotlin 1.0
            call.response.header(HttpHeaders.LastModified, date)
            call.response.header(HttpHeaders.ETag, etag)
            call.response.lastModified(date)
            call.sendHttpBinResponse()
        }

        // This route sets the Cache Control header to have a maxAge to [n] seconds.
        get("/cache/{n}") {
            val n = call.parameters["n"]!!.toInt()
            val cache = CacheControl.MaxAge(maxAgeSeconds = n, visibility = CacheControl.Visibility.Public)
            call.response.cacheControl(cache)
            call.sendHttpBinResponse()
        }

        // Returns the User-Agent header sent by the client.
        get("/user-agent") {
            call.sendHttpBinResponse {
                clear()
                `user-agent` = call.request.header("User-Agent")
            }
        }

        // Returns a HTTP status code based on the {status} url parameter.
        get("/status/{status}") {
            val status = call.parameters["status"]?.toInt() ?: 0
            call.respond(HttpStatusCode.fromValue(status))
        }

        // Returns a HTML page with a ul list of [n] links and the [m]th link will be selected (unclickable).
        get("/links/{n}/{m?}") {
            try {
                val nbLinks = call.parameters["n"]!!.toInt()
                val selectedLink = call.parameters["m"]?.toInt() ?: 0
                call.respondHtml {
                    generateLinks(nbLinks, selectedLink)
                }
            } catch (e: Throwable) {
                call.respondHtml(status = HttpStatusCode.BadRequest) {
                    invalidRequest("$e")
                }
            }
        }

        // Responds with a text saying that you shouldn't be here.
        get("/deny") {
            call.respondText(ANGRY_ASCII)
        }

        // Throws an exception that will be handled by the [StatusPages] feature installed and configured above.
        get("/throw") {
            throw RuntimeException("Endpoint /throw thrown a throwable")
        }

        // Responds with the headers specified by the queryParameters. So for example
        //
        // - /response-headers?Location=/deny -- Would generate a header 'Location' to redirect to '/deny'
        //
        // Also it responds with a JSON with the specified query parameters
        get("/response-headers") {
            val params = call.request.queryParameters
            val requestedHeaders = params.flattenEntries().toMap()
            for ((key, value) in requestedHeaders) {
                call.response.header(key, value)
            }
            val content = TextContent(gson.toJson(params), ContentType.Application.Json)
            call.respond(content)
        }

        // Generates a redirection chain. Just like a recursive function.
        // - /redirect/10  -- would redirect to /redirect/9.
        // - /redirect/0   -- wouldn't redirect
        // This is useful for testing maximum redirections from HTTP clients.
        //重定向
        get("/redirect/{n}") {
            val n = call.parameters["n"]!!.toInt()
            if (n == 0) {
                call.sendHttpBinResponse()
            } else {
                //重定向 到另一个路径
                call.respondRedirect("/redirect/${n - 1}")
            }
        }


        // Returns the list of raw cookies sent by the client
        get("/cookies") {
            val rawCookies = call.request.cookies.rawCookies
            call.sendHttpBinResponse {
                clear()
                cookies = rawCookies
            }
        }

        // Generates a response that will instruct to set cookies based on the query parameters sent by the client.
        get("/cookies/set") {
            val params = call.request.queryParameters.flattenEntries()
            for ((key, value) in params) {
                call.response.cookies.append(name = key, value = value, path = "/")
            }
            val rawCookies = call.request.cookies.rawCookies
            call.sendHttpBinResponse {
                clear()
                cookies = rawCookies + params.toMap()
            }
        }

        // Generates a response that will set expired cookies based on the query parameters sent by the client.
        get("/cookies/delete") {
            val params = call.request.queryParameters.names()
            val rawCookies = call.request.cookies.rawCookies
            for (name in params) {
                call.response.cookies.appendExpired(name, path = "/")
            }
            call.sendHttpBinResponse {
                clear()
                cookies = rawCookies.filterKeys { key -> key !in params }
            }
        }

        // Register a route that uses the basic Authentication feature to request a user/password to the user when
        // no user/password is provided or is invalid, and handles the request if the authentication is valid.
        route("/basic-auth") {
            authentication {
                basic("ktor-samples-httpbin") {
                    validate {
                        hashedUserTable.authenticate(it)
                    }
                }
            }
            get {
                call.sendHttpBinResponse()
            }
            get("{user}/{password}") {
                call.sendHttpBinResponse()
            }
        }

        // Always generate an unauthorized response.
        get("/hidden-basic-auth/{user}/{password}") {
            call.respond(HttpStatusCode.Unauthorized)
        }

        // Instead of replying with with a content at once, uses chunked encoding to send a lorenIpsum [n] times
        // serving a chunk per loren ipsum.
        get("/stream/{n}") {
            val lorenIpsum =
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
            val times = call.parameters["n"]!!.toInt()
            call.respondTextWriter {
                //连续写入
                repeat(times) {
                    write(lorenIpsum)
                    flush()
                }
            }
        }

        // Responds the request after [n] seconds (where n is between 0 and 10 inclusive)
        get("/delay/{n}") {
            val n = call.parameters["n"]!!.toLong()
            require(n in 0..10) { "Expected a number of seconds between 0 and 10" }
            delay(n)
            call.sendHttpBinResponse()
        }

        // Sends a chunked response of [numbytes] '*' bytes over [duration] seconds with the specified http [code].
        // This will delay each chunk according.
        // time curl --no-buffer "http://127.0.0.1:8080/drip?duration=5&numbytes=5000&code=200"
        get("/drip") {
            val duration = call.parameters["duration"]?.toDoubleOrNull() ?: 2.0
            val numbytes = call.parameters["numbytes"]?.toIntOrNull() ?: (10 * 1024 * 1024)
            val code = call.parameters["code"]?.toIntOrNull() ?: 200
            val bias = 2
            call.respondTextWriter(status = HttpStatusCode.fromValue(code)) {
                val start = System.currentTimeMillis()
                var now = start
                for (n in 0 until numbytes) {
                    val expected = start + ((n + 1) * duration * 1000).toInt() / numbytes
                    val delay = expected - now
                    if (now <= expected) {
                        flush()
                        delay(delay)
                    }

                    write('*'.toInt())
                    now = System.currentTimeMillis()
                }
            }
        }

        // Gets a response with [n] random bytes from an insecure random source.
        get("/bytes/{n}") {
            val n = call.parameters["n"]!!.toInt()
            val r = Random()
            val buffer = ByteArray(n) { r.nextInt().toByte() }
            call.respond(buffer)
        }


        // Handls all the other non-matched routes returning a 404 not found.
        route("{...}") {
            handle {
                val error = HttpBinError(
                    code = HttpStatusCode.NotFound,
                    request = call.request.local.uri,
                    message = "NOT FOUND"
                )
                call.respond(error)
            }
        }

    }
}


