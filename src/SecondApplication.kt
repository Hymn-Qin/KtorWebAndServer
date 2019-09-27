package com.geely.gic.hmi

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
fun Application.testModule(testing: Boolean = false) {
    /**
     * Here we create and start a Netty embedded server listening to the port 8080
     * and define the main application module inside the specified lambda.
     */
//    io.ktor.server.engine.embeddedServer(Netty, port = 8080) {
//
//    }.start(wait = true)

    /**
     *  some Netty  开放多个端口
     */
    val env = applicationEngineEnvironment {
        module {
//            main()
        }
        // Private API
        connector {
            host = "0.0.0.1"
            port = 9090
        }
        // Public API
        connector {
            host = "0.0.0.1"
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