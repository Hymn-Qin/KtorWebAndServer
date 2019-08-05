package com.geely.gic.hmi

import com.geely.gic.hmi.data.Session
import data.IndexData
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.content.*
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.directorySessionStorage
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream

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
        cookie<Session>("Session", directorySessionStorage(File(".sessions"))) {
            cookie.path = "/"
        }
        //directorySessionStorage() 来自 Ktor 的 Session 库，并且需要注意的是，directorySessionStorage()也是一个 Experimental 的 API，需要加入注解来使其能够顺利编译
        //表示了这个 Session 可以在文件系统里保存，并且作用范围是全站，即以 / 为路径的所有请求。
        // 这意味着我们可以通过请求路径来进行 Session 的隔离。

        //只需要session
        cookie<Session>("Session")
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

        //5.模板返回
        get("/html-freemarker") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3, 4, 5, 6, 7))), ""))
        }

        //6.Session
        get("/session") {
            
        }
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