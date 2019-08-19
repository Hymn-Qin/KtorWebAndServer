package com.geely.gic.hmi

import chat.chat
import com.fasterxml.jackson.databind.SerializationFeature
import com.geely.gic.hmi.data.dao.userDao
import com.geely.gic.hmi.http.*
import com.geely.gic.hmi.data.dao.videoDao
import com.geely.gic.hmi.data.model.HttpBinError
import com.geely.gic.hmi.data.model.InvalidCredentialsException
import com.geely.gic.hmi.data.model.Reply
import com.geely.gic.hmi.security.authentication
import com.geely.gic.hmi.http.upload
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
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import io.ktor.util.getDigestFunction
import io.ktor.util.hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@Location("/user")
class Users {
    @Location("/login")
    data class Login(val userId: String = "", val password: String = "", val error: String = "")

    @Location("/register")
    data class Register(
        val userId: String = "",
        val displayName: String = "",
        val email: String = "",
        val error: String = ""
    )

    @Location("/{user}")
    data class UserInfo(val user: String, val error: String = "")

    @Location("/update")
    data class UserUpdate(
        val userId: String = "",
        val error: String = ""
    )

    @Location("/logout")
    class Logout()
}

@Location("image")
class Load {

}

@Location("/video")
class Video {
    /**
     * Location for a specific video stream by [id].
     */
    @Location("/{id}")
    data class VideoStream(val id: Long)

    /**
     * Location for a specific video page by [id].
     */
    @Location("/{authorId}/{id}")
    data class VideoPage(val authorId: String, val id: Long)

    /**
     * Location for uploading videos.
     */
    @Location("/upload")
    class Upload()

    /**
     * The index root page with a summary of the site.
     */
    @Location("")
    class Index()
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
fun Application.module(testing: Boolean = false) {

    logger.info("Http started")

    // Obtains the app config key from the application.conf file.
    // Inside that key, we then read several configuration properties
    // with the [session.cookie], the [key] or the [upload.dir]
    val appConfig = environment.config.config("app")
    val sessionCookieConfig = appConfig.config("session.cookie")
    val key: String = sessionCookieConfig.property("key").getString()
    val sessionkey = hex(key)

    // We create the folder and a [Database] in that folder for the configuration.
    val usersDirPath: String = appConfig.property("users.dir").getString()
    val uploadDirPath: String = appConfig.property("upload.dir").getString()
    //初始化内存数据库
    val daoVideo = videoDao(uploadDirPath)
    val dao = userDao(usersDirPath)

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
//    install(Compression)
    install(Compression) {
        default()
        excludeContentType(ContentType.Video.Any)
    }
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
    // 允许回复任意对象  并转换为json
    // Based on the Accept header, allows to reply with arbitrary objects converting them into JSON
    // when the client accepts it.
    install(ContentNegotiation) {
        register(ContentType.Application.Json, GsonConverter())
//        jackson {
//            enable(SerializationFeature.INDENT_OUTPUT) // 美化输出 JSON
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
//        exception<Throwable> { cause ->
//            environment.log.error(cause)
//            val error = HttpBinError(
//                code = HttpStatusCode.InternalServerError,
//                request = call.request.local.uri,
//                message = cause.toString(),
//                cause = cause
//            )
//            call.respond(error)
//        }

        //捕获无效的凭据异常
        exception<InvalidCredentialsException> { exception ->
            environment.log.error(exception)
            val error = HttpStatusCode.Unauthorized
            val reply = Reply(
                error.value,
                exception.message ?: "",
                "null"
            )
            call.respond(reply)

        }
    }

    // Fake Authorization with user:password "test:test"
    val hashedUserTable = UserHashedTableAuth(
        getDigestFunction("SHA-256") { "ktor${it.length}" },
        table = mapOf(
            "test" to Base64.getDecoder().decode("GSjkHCHGAxTTbnkEDBbVYd+PUFRlcWiumc4+MWE9Rvw=") // sha256 for "test"
        )
    )

    //身份认证
    val simpleJWT = authentication(hashedUserTable)

    routing {
        login(simpleJWT, dao, httpClient)
        register(dao)
        userInfo(dao)
        image()

        upload(daoVideo)
        videos(daoVideo)
        styles()
    }

    //webSocket
    chat(testing)
}



