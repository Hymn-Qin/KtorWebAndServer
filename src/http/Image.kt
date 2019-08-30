package com.geely.gic.hmi.http

import com.geely.gic.hmi.utils.ImageConfig
import io.ktor.http.ContentType
import io.ktor.http.content.resource
import io.ktor.routing.Route
import io.ktor.routing.accept
import io.ktor.routing.route

fun Route.image() {
    route("/image") {

        val imageConfigs = listOf(
            ImageConfig("jpeg", ContentType.Image.JPEG, "jackal.jpg"),
            ImageConfig("png", ContentType.Image.PNG, "pig_icon.png"),
            ImageConfig("svg", ContentType.Image.SVG, "svg_logo.svg"),
            ImageConfig("webp", ContentType("image", "webp"), "wolf_1.webp"),
            ImageConfig("any", ContentType.Image.Any, "jackal.jpg")
        )
        for ((path, contentType, filename) in imageConfigs) {
            // Serves this specific file in the specific format in the route when the 'Accept' header makes it the best match.
            // So for example a Chrome browser would receive a WEBP image, while another browser like Internet Explorer would receive a JPEG.
            accept(contentType) {
                resource("", "static/$filename")
            }
            // As a fallback, we also serve the file independently on the Accept header, in the `/image/format` route.
            resource(path, "static/$filename")
        }
    }

}