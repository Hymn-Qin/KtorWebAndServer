package com.geely.gic.hmi.http.utils

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.Video
import com.geely.gic.hmi.http.data.YouKubeSession
import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.html.HtmlContent
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.content.CachingOptions
import io.ktor.http.content.Version
import io.ktor.http.content.caching
import io.ktor.http.content.versions
import io.ktor.locations.locations
import io.ktor.locations.url
import io.ktor.response.respond
import io.ktor.util.date.GMTDate
import kotlinx.html.*
import web.MainCss

/**
 * Function that generates HTML for the structure of the page and allows to provide a [block] that will be placed
 * in the content place of the page.
 */
suspend fun ApplicationCall.respondDefaultHtml(versions: List<Version>, visibility: CacheControl.Visibility, title: String = "You Kube", block: DIV.() -> Unit) {
    val content = HtmlContent(HttpStatusCode.OK) {
        val session = YouKubeSession("123")//sessions.get<YouKubeSession>()
        head {
            title { +title }
            styleLink("http://yui.yahooapis.com/pure/0.6.0/pure-min.css")
            styleLink("http://yui.yahooapis.com/pure/0.6.0/grids-responsive-min.css")
            //添加自定义样式
            styleLink(url(MainCss()) {
                protocol = URLProtocol.createOrDefault(request.origin.scheme)
            })
        }
        body {
            div("pure-g") {
                div("sidebar pure-u-1 pure-u-md-1-4") {
                    div("header") {
                        div("brand-title") { +title }
                        div("brand-tagline") {
                            if (session != null) {
                                +session.userId
                            }
                        }

                        nav("nav") {
                            ul("nav-list") {
                                li("nav-item") {
                                    if (session == null) {
                                        a(classes = "pure-button", href = locations.href(Users.Login())) { +"Login" }
                                    } else {
                                        a(classes = "pure-button", href = locations.href(Video.Upload())) { +"Upload" }
                                    }
                                }
                                li("nav-item") {
                                    a(classes = "pure-button", href = locations.href(Video.Index())) { +"Watch" }
                                }
                            }
                        }
                    }
                }

                div("content pure-u-1 pure-u-md-3-4") {
                    block()
                }
            }
        }
    }
    content.versions = versions
    content.caching = CachingOptions(
            cacheControl = CacheControl.MaxAge(3600 * 24 * 7, mustRevalidate = true, visibility = visibility, proxyMaxAgeSeconds = null, proxyRevalidate = false),
            expires = (null as? GMTDate?)
    )
    respond(content)
}


