package com.geely.gic.hmi

import com.geely.gic.hmi.data.dao.userDao
import com.geely.gic.hmi.data.model.Session
import com.geely.gic.hmi.security.hashFunction
import com.geely.gic.hmi.security.hashKey
import com.geely.gic.hmi.utils.gson
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.freemarker.FreeMarker
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import io.ktor.util.hex
import io.ktor.websocket.WebSockets
import web.index
import web.login
import web.register
import web.styles
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

//main函数
fun main(args: Array<String>): Unit =
    //创建一个内嵌Netty的服务器
    io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    environment.log.info("Web started")

    // Obtains the youkube config key from the application.conf file.
    // Inside that key, we then read several configuration properties
    // with the [session.cookie], the [key] or the [upload.dir]
    val appConfig = environment.config.config("app")
    val sessionCookieConfig = appConfig.config("session.cookie")
    val key: String = sessionCookieConfig.property("key").getString()
    val sessionkey = hex(key)

    // We create the folder and a [Database] in that folder for the configuration [upload.dir].
    val usersDirPath: String = appConfig.property("users.dir").getString()
    val uploadDirPath: String = appConfig.property("upload.dir").getString()
    val uploadDir = File(uploadDirPath)
    if (!uploadDir.mkdirs() && !uploadDir.exists()) {
        throw IOException("Failed to create directory ${uploadDir.absolutePath}")
    }

//    val database = VideoDatabase(uploadDir)


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
    // Supports for Range, Accept-Range and Content-Range headers 启用部分内容特性 大文件和视频
    install(PartialContent)
    //8.安装路由 当我们的路由很多的时候，如果都写在一个文件里，不仅仅文件回变得很大，而且不利于维护和团队协作。所以有了另一个模块 locations
    install(Locations)
    //5.安装FreeMarker模板
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    // Configure the session to be represented by a [Sessions],
    // using the SESSION cookie to store it, and transforming it to be authenticated with the [hashKey].
    // it is sent in plain text, but since it is authenticated can't be modified without knowing the secret [hashKey].
    install(Sessions) {
        //        cookie<Session>("Session", directorySessionStorage(File(".sessions"))) {
//            cookie.path = "/"
//        }
        //1).用cookie的方式 将 session 保存在本地
//        cookie<Session>("Session", directorySessionStorage(File(".sessions"))) {
//            cookie.path = "/"
//        }
        //directorySessionStorage() 来自 Ktor 的 Session 库，并且需要注意的是，directorySessionStorage()也是一个 Experimental 的 API，需要加入注解来使其能够顺利编译
        //表示了这个 Session 可以在文件系统里保存，并且作用范围是全站，即以 / 为路径的所有请求。
        // 这意味着我们可以通过请求路径来进行 Session 的隔离。

        //只需要session 不需要保存的情况
//        cookie<Session>("Session")
        //2).另外一种请求方式，即把相关的数据放在 Header
        //通常是用于 API 或 XHR 请求，这个时候我们可以使用 header() 来描述 Session
//        header<Session>("Session") {
//            transform(SessionTransportTransformerMessageAuthentication(SecretKeySpec(key, "HmacSHA256")))
//            //key 是一个 ByteArray 对象，也就是加密用的 key，它可以是任意组合的 byte 串。后面的 HmacSHA256 是采用的算法
//            // Ktor 官方文档内，用于 Header 的 transform 是 SessionTransportTransformerDigest，而这个类并不安全
//            // 为了安全起见，应当使用此处的 SessionTransportTransformerMessageAuthentication 并配合相应的加密手段。
//        }

        //3).上面都两种都是将cookie写入服务端，将前两种结合，就拥有了写到客户端的 Cookie 了  将cookie保存在客户端
        cookie<Session>("Session") {
            transform(SessionTransportTransformerMessageAuthentication(hashKey))
        }
    }

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
//            val error = HttpBinError(
//                code = HttpStatusCode.InternalServerError,
//                request = call.request.local.uri,
//                message = cause.toString(),
//                cause = cause
//            )
//            call.respond(error)
        }
    }
    //7.安装webSocket
    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
    }

    //初始化内存数据库
    val dao = userDao(usersDirPath)

    val httpClient = HttpClient(Apache) {
        install(JsonFeature)

    }

    routing {

        //1.静态配置  在 get("/") 时，返回 index.html 的内容，而此时并不需要明确的写出 get("/")，只需要写 defaultResource() 即可
        static {
            defaultResource("index.html", "web")
            resources("web")
        }
        ///static 静态文件夹  里面保存图片、资源文件等
        static("/static") {
            resources("static")
        }

        index(dao = dao)
        login(dao = dao, client = httpClient, hash = hashFunction)
        register(dao = dao, client = httpClient, hash = hashFunction)
//        userPage(httpClient)
//        image()
//
//        upload(database, uploadDir)
//        videos(database)
        styles()
    }

    //webSocket
//    chat(testing)
}


/**
 * The index root page with a summary of the site.
 */
@Location("/")
class Index()

@Location("/user")
class Users {
    @Location("/")
    data class Login(
        val userId: String = "",
        val password: String = "",
        val error: String? = null)

    @Location("/register")
    data class Register(
        val userId: String = "",
        val displayName: String = "",
        val email: String = "",
        val password: String = "",
        val error: String? = null
    )

    @Location("/{userId}")
    data class UserPage(
        val userId: String,
        val error: String? = null)

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
}
