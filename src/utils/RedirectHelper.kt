package com.geely.gic.hmi.utils

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.features.StatusPages
import io.ktor.features.origin
import io.ktor.locations.Location
import io.ktor.locations.locations
import io.ktor.locations.url
import io.ktor.response.respondRedirect
import io.ktor.sessions.CurrentSession
import io.ktor.sessions.get
import kotlin.reflect.KClass



/**
 * Utility for performing non-permanent redirections using a typed [location] whose class is annotated with [Location].
 */
suspend fun ApplicationCall.respondRedirect(location: Any) = respondRedirect(url(location), permanent = false)

/**
 * 重定向
 * Allows to respond with a absolute redirect from a typed [location] instance of a class annotated
 * with [Location] using the Locations feature.
 */
suspend fun ApplicationCall.redirect(location: Any): Nothing = redirect(url(location), permanent = false)


/**
 * 出错 去重定向
 * Exception used to be captured by [StatusPages] to perform a redirect.
 */
class RedirectException(val path: String, val permanent: Boolean) : Exception()

/**
 * 在这里抛出异常 [RedirectException] 并传递重定向路径
 * Global function that throws a [RedirectException], to be catched by the [StatusPages] feature to perform a redirect
 * to [path].
 */
fun redirect(path: String, permanent: Boolean = false): Nothing = throw RedirectException(path, permanent)

/**
 * Extension method for configuring [StatusPages] that encapsulates the functionality of catching
 * the [RedirectException] and actually performing a redirection.
 */
fun StatusPages.Configuration.registerRedirections() {
    exception<RedirectException> { cause ->
        application.log.error(cause.path)
        call.respondRedirect(cause.path, cause.permanent)
    }
}

////////////////////////////////
// Session Not Found Utilities
////////////////////////////////

/**
 * Exception used to be captured by [StatusPages] (or catched) to notify that the session couldn't be found,
 * so the application can do things like redirect. It stores the session that couldn't be retrieved to be able
 * to have different behaviours.
 */
class SessionNotFoundException(val clazz: KClass<*>) : Exception()


/**
 * 判断 Session 有效性 无效则抛出异常 并 [registerSessionNotFoundRedirect] 重定向
 * Convenience method to try to get a exception of type [T], or to throw a [SessionNotFoundException] to
 * handle it either by catching or by using the [StatusPages] feature.
 */
inline fun <reified T> CurrentSession.getOrThrow(): T =
    this.get<T>() ?: throw SessionNotFoundException(T::class)

/**
 * Extension method for configuring [StatusPages] that encapsulates the functionality of catching
 * the [SessionNotFoundException] to redirect to the [path] page in the case of the session [T].
 */
inline fun <reified T> StatusPages.Configuration.registerSessionNotFoundRedirect(path: String) {
    exception<SessionNotFoundException> { cause ->
        if (cause.clazz == T::class) call.respondRedirect(path)
    }
}