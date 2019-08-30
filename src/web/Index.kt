package web

import com.geely.gic.hmi.Index
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.model.Session
import io.ktor.application.call
import io.ktor.locations.get
import io.ktor.routing.Route
import io.ktor.sessions.get
import io.ktor.sessions.sessions

/**
 * Register the index route of the website.
 */
fun Route.index(dao: DAOFacade) {
    // Uses the location feature to register a get route for '/'.
    get<Index> {
        // Tries to get the user from the session (null if failure)
        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }

        //模板方式
//        call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3, 4, 5, 6, 7))), ""))


        // Obtains several list of kweets using different sortings and filters.
        val top = dao.top(10).map { dao.getKweet(it) }
        val latest = dao.latest(10).map { dao.getKweet(it) }

        // Generates an ETag unique string for this route that will be used for caching.
        val etagString = user?.userId + "," + top.joinToString { it.id.toString() } + latest.joinToString { it.id.toString() }
        val etag = etagString.hashCode()

        // Uses FreeMarker to render the page.
//        call.respond(FreeMarkerContent("index.ftl", mapOf("top" to top, "latest" to latest, "user" to user), etag.toString()))

        //            call.resolveResource("index.html", "web")?.let { index ->
//                call.respond(index)
//            }
    }
}
