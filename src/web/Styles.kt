package web

import com.geely.gic.hmi.utils.respondCss
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.css.Color
import kotlinx.css.body
import kotlinx.css.em
import kotlinx.css.p

@Location("/styles/main.css")
class MainCss()

/**
 * Register the styles, [MainCss] route (/styles/main.css)
 */
fun Route.styles() {
    /**
     * On a GET request to the [MainCss] route, it returns the `blog.css` file from the resources.
     *
     * Here we could preprocess or join several CSS/SASS/LESS.
     */
    get<MainCss> {
        call.respond(call.resolveResource("static/blog.css")!!)

//        call.respondCss {
//            body {
//                backgroundColor = Color.red
//            }
//            p {
//                fontSize = 2.em
//            }
//            rule("p.myclass") {
//                color = Color.blue
//            }
//        }
    }
}
