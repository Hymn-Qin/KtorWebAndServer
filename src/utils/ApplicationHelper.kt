package com.geely.gic.hmi.utils

import com.geely.gic.hmi.data.model.Session
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.sessions.sessions
import kotlinx.css.CSSBuilder

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

