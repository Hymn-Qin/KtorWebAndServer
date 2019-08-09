package chat

import chat.data.UserSession
import com.geely.gic.hmi.chat.ChatServer
import com.geely.gic.hmi.data.model.Session
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.sessions.*
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

//main函数
fun main(args: Array<String>): Unit =
    //创建一个内嵌Netty的服务器
    io.ktor.server.netty.EngineMain.main(args)

const val SESSION_REGISTER_NAME = "ktor-chat-sample"
inline val WebSocketServerSession.session: UserSession? get() = try { call.sessions.get(SESSION_REGISTER_NAME) as? UserSession } catch (th: Throwable) { null }
inline val PipelineContext<*, ApplicationCall>.session: UserSession? get() = try { context.sessions.get(
    SESSION_REGISTER_NAME
) as? UserSession } catch (th: Throwable) { null }
fun PipelineContext<*, ApplicationCall>.setSession(us: UserSession) = context.sessions.set(SESSION_REGISTER_NAME, us)

private val server = ChatServer()

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val logger by lazy { LoggerFactory.getLogger(Application::class.java) }

    val client = HttpClient(Apache) {

    }

    //6.安装Session
    install(Sessions) {
        //1).用cookie的方式 将 session 保存在本地
        cookie<Session>("Session", directorySessionStorage(File(".sessions"))) {
            cookie.path = "/"
        }
    }

    //7.安装webSocket
    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
    }

    //路由代码块 配置路由特性 在代码块中指定路径与HTTP方法定义路由
    routing {
        //7.webSocket 聊天室
        webSocket("/ws") {
            val ses = session
            if (ses == null) {
                close(Throwable(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session").toString()) )
                return@webSocket
            }
            server.memberJoin(ses, this)
            try {
                incoming.consumeEach {
                    if (it is Frame.Text) {
                        server.receivedMessage(ses, it.readText())
                    }
                }
            } finally {
                server.memberLeft(ses, this)
            }
        }
    }
}
