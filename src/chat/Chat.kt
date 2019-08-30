package chat

import chat.data.model.UserSession
import com.geely.gic.hmi.chat.ChatServer
import chat.data.model.ChatSession
import io.ktor.application.*
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import java.time.Duration

const val SESSION_REGISTER_NAME = "ktor-chat-sample"
inline val WebSocketServerSession.session: UserSession?
    get() = try {
        call.sessions.get(SESSION_REGISTER_NAME) as? UserSession
    } catch (th: Throwable) {
        null
    }
inline val PipelineContext<*, ApplicationCall>.session: UserSession?
    get() = try {
        context.sessions.get(SESSION_REGISTER_NAME) as? UserSession
    } catch (th: Throwable) {
        null
    }

fun PipelineContext<*, ApplicationCall>.setSession(us: UserSession) = context.sessions.set(SESSION_REGISTER_NAME, us)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.chat(testing: Boolean = false) {

    /**
     * First we install the features we need. They are bound to the whole application.
     * Since this method has an implicit [Application] receiver that supports the [install] method.
     */
    // This adds automatically Date and Server headers to each response, and would allow you to configure
    // additional headers served to each response.
//    install(DefaultHeaders)
    // This uses use the logger to log every call (request/response)
//    install(CallLogging)
    // This installs the websockets feature to be able to establish a bidirectional configuration
    // between the server and the client
    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
    }
    install(Sessions) {
        cookie<ChatSession>("SESSION")
    }
    // 拦截器 如果没有可用的会话，它将在每个请求中创建一个特定的会话。
    // This adds an interceptor that will create a specific session in each request if no session is available already.
    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<ChatSession>() == null) {
            call.sessions.set(ChatSession(generateNonce()))
        }
    }

    val server = ChatServer()

    //路由代码块 配置路由特性 在代码块中指定路径与HTTP方法定义路由
    routing {
        //7.webSocket 聊天室
        webSocket("/ws") {
            val ses = call.sessions.get<ChatSession>()
            if (ses == null) {
                close(Throwable(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session").toString()))
                return@webSocket
            }
            server.memberJoin(ses.id, this)
            try {
                incoming.consumeEach {
                    if (it is Frame.Text) {
                        server.receivedMessage(ses.id, it.readText())
                    }
                }
            } finally {
                server.memberLeft(ses.id, this)
            }
        }

        // This defines a block of static resources for the '/' path (since no path is specified and we start at '/')
        static ("/chat"){
            // This marks index.html from the 'web' folder in resources as the default file to serve.
            defaultResource("chat.html", "web")
            // This serves files from the 'web' folder in the application resources.
            resources("web")
        }
    }
}

