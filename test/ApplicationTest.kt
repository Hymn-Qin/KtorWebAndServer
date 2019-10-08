import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.InternalAPI
import io.ktor.util.StringValues
import io.ktor.util.StringValuesImpl
import kotlin.test.*

class ApplicationTest {
    @InternalAPI
    @Test
    fun testRoot() {
//        withTestApplication({ chat.module(testing = true) }) {
//            handleRequest(HttpMethod.Get, "/").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("HELLO WORLD!", response.content)
//            }
//        }
        val list: StringValues = StringValuesImpl(values = mapOf("name" to listOf("1")))
        val name = list.getOrThrow("name")
        println(name)
    }
}

fun StringValues.getOrThrow(name: String): String = get(name).let {
    when {
        it.isNullOrBlank() -> throw Exception("Invalid $name")
        else -> it
    }
}


fun main(args: Array<String>) {
    val env = applicationEngineEnvironment {
        module {
            main()
        }
        // Private API
        connector {
            host = "127.0.0.1"
            port = 9090
        }
        // Public API
        connector {
            host = "0.0.0.0"
            port = 8080
        }
    }
    embeddedServer(Netty, env).start(true)
}

fun Application.main() {
    routing {
        get("/") {
            if (call.request.local.port == 8080) {
                call.respondText("Connected to public api")
            } else {
                call.respondText("Connected to private api")
            }
        }
    }
}