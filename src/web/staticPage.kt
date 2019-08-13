package web

import io.ktor.application.Application
import io.ktor.http.content.*
import io.ktor.routing.route
import io.ktor.routing.routing
import java.io.File

fun Application.staticPage() {
    // Folder from the File System that we are going to use to serve static files.
    val staticfilesDir = File("resources/static")
    require(staticfilesDir.exists()) { "Cannot find ${staticfilesDir.absolutePath}" }
    routing {
        //静态页面
        // A static route where the 'static' folder is the base.
        static {
            staticBasePackage = "static"

            defaultResource("index.html")
            resource("xml", "sample.xml")
            resource("encoding/utf8", "UTF-8-demo.html")
            resource("html", "moby.html")
            resource("robots.txt")
            resource("forms/post", "forms-post.html")
            resource("postman", "httpbin.postman_collection.json")
            resource("httpbin.js")

            // And for the '/static' path, it will serve the [staticfilesDir].
            route("static") {
                files(staticfilesDir)
            }
        }
    }
}
