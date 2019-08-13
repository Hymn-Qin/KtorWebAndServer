package com.geely.gic.hmi

import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.dao.initDao
import com.geely.gic.hmi.http.httpModule
import com.geely.gic.hmi.http.login
import com.geely.gic.hmi.http.register
import com.geely.gic.hmi.http.security.authentication
import http.data.model.HttpBinError
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.UserHashedTableAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import io.ktor.util.getDigestFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@Location("/user")
class Users{
    @Location("/login")
    data class Login(val userId: String = "", val error: String = "")

    @Location("/register")
    data class Register(
        val userId: String = "",
        val displayName: String = "",
        val email: String = "",
        val error: String = ""
    )

    @Location("/{user}")
    data class UserPage(val user: String)

    @Location("/logout")
    class Logout()
}

//main函数
fun main(args: Array<String>): Unit =
    //创建一个内嵌Netty的服务器
    io.ktor.server.netty.EngineMain.main(args)

val logger: Logger by lazy { LoggerFactory.getLogger("Http") }

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false, dao: DAOFacade = initDao()) {

    logger.info("Http started")

    //身份认证
    val simpleJWT = authentication()

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

    routing {
        login(simpleJWT, dao)
        register(dao)
    }
}



