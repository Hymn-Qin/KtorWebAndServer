package com.geely.gic.hmi

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.geely.gic.hmi.data.model.Session
import com.geely.gic.hmi.data.model.IndexData
import com.geely.gic.hmi.data.model.PostSnippet
import com.geely.gic.hmi.data.model.Snippet
import com.geely.gic.hmi.login.login
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.features.ContentNegotiation
import io.ktor.features.PartialContent
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.content.*
import io.ktor.jackson.jackson
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*

//main函数
fun main(args: Array<String>): Unit =
    //创建一个内嵌Netty的服务器
    io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val logger by lazy { LoggerFactory.getLogger(Application::class.java) }

    val client = HttpClient(Apache) {

    }

//    val htmlContent = client.get<String>("")

    //在 Ktor 中由于解藕的存在，各个功能均是被安装进去的
    //5.安装FreeMarker模板
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    //6.安装Session
    install(Sessions) {
        //        cookie<Session>("Session", directorySessionStorage(File(".sessions"))) {
//            cookie.path = "/"
//        }
        //directorySessionStorage() 来自 Ktor 的 Session 库，并且需要注意的是，directorySessionStorage()也是一个 Experimental 的 API，需要加入注解来使其能够顺利编译
        //表示了这个 Session 可以在文件系统里保存，并且作用范围是全站，即以 / 为路径的所有请求。
        // 这意味着我们可以通过请求路径来进行 Session 的隔离。

        //只需要session
        cookie<Session>("Session")
    }

    //8.启用部分内容特性 大文件和视频
    install(PartialContent) {
    }

    val simpleJWT = SimpleJWT("my-super-secret-for-jwt")
    //身份认证特性
    install(Authentication) {
        //基本的身份认证
//        basic {
//            realm = "myrealm"
//            validate {
//                if (it.name == "user" && it.password == "password")
//                    UserIdPrincipal("user")
//                else
//                    null
//            }
//        }

        //JWT令牌取代基本的basic身份认证  安装并配置 JWT
        jwt {
            verifier(simpleJWT.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    //9.内容协商 json
    install(ContentNegotiation) {
        jackson {
        }
    }

    //路由代码块 配置路由特性 在代码块中指定路径与HTTP方法定义路由
    routing {
        //1.静态配置  在 get("/") 时，返回 index.html 的内容，而此时并不需要明确的写出 get("/")，只需要写 defaultResource() 即可
        static {
            defaultResource("index.html", "web")
            resources("web")
        }
        //1.实际路径  处理GET请求 和上面的静态配置相同  返回一个html页面
//        get("/") {
//            call.resolveResource("index.html", "web")?.let { index ->
//                call.respond(index)
//            }
//        }
        //2.返回文字
        get("/text") {
            call.respondText { "返回文字" }
        }

        //3.获取携带的参数
        get("/demo") {
            val map = call.parameters.entries()
            logger.info("get /demo 获取参数：{}", map)
            call.respondText { map.toString() }
        }

        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.red
                }
                p {
                    fontSize = 2.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }

        //4.post 请求
        post("/upload") {
            //post 请求获取参数
            val parts = call.receiveMultipart()
            parts.forEachPart {
                when (it) {
                    is PartData.FormItem -> {
                        logger.info("post /upload 获取参数：{}", "${it.name} = ${it.value}")
                    }
                    is PartData.FileItem -> {
                        val file = File("", "file -${System.currentTimeMillis()}")
                        it.streamProvider().use { input ->
                            file.outputStream().buffered().use { output ->
                                input.copyToSuspend(output)
                            }

                        }
                    }
                }
                it.dispose
            }

        }

        ///static 静态文件夹  里面保存图片、资源文件等
        static("/static") {
            resources("static")
        }
        //5.模板返回
        get("/html-freemarker") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3, 4, 5, 6, 7))), ""))
        }

        //6.Session
        get("/session") {
            val session = call.sessions.get("Session")
            if (session != null) {
                call.respondText("User is logged")
            } else {
                call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3))), ""))
            }
        }

        //8.大文件和视频特性
        route("/login") {
            get {
                call.respond(FreeMarkerContent("login.ftl", null))
            }
            post {
                val post = call.receiveParameters()
                if (post["username"] != null && post["username"] == post["password"]) {
//                    call.respondText("OK")
                    //重定向到首页
                    call.respondRedirect("/", permanent = false)
                } else {
                    call.respond(FreeMarkerContent("login.ftl", mapOf("error" to "Invalid login")))
                }
            }
        }

        /**
         * Http 后端服务
         */
        //9，Http 内容协商
        val snippets = Collections.synchronizedList(
            mutableListOf(
                Snippet(user = "test", text = "hello"),
                Snippet(user = "test", text = "world")
            )
        )
//        get("/snippets") {
//            //            call.respond(mapOf("OK" to true))
//            //回复json字符串
//            //
//            call.respond(mapOf("snippets" to synchronized(snippets) {
//                snippets.toList()
//            }))
//        }
//
//        post("/snippets") {
//            val post = call.receive<PostSnippet>()
//            //接收数据{"snippet": {"text" : "mysnippet"}}
//            snippets += Snippet(post.snippet.text)
//            call.respond(mapOf("OK" to true))
//        }

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

        login(simpleJWT)

    }

}

fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

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

open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}