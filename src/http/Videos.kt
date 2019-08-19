package com.geely.gic.hmi.http

import com.geely.gic.hmi.Video
import com.geely.gic.hmi.data.dao.DAOVideoCache
import io.ktor.application.call
import io.ktor.features.PartialContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.LocalFileContent
import io.ktor.http.fromFilePath
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import java.io.File

/**
 * Register video-related routes: [Video.VideoStream] (/), [VideoPage] (/video/page/{id}) and [VideoStream] (/video/{id})
 */
fun Route.videos(daoCache: DAOVideoCache) {
    /**
     * Returns the bits of the video specified by [VideoStream.id] or [HttpStatusCode.NotFound] if the video is not found.
     * It returns a [LocalFileContent] that works along the installed [PartialContent] feature to support getting chunks
     * of the content, and allowing the navigator to seek the video even if the video content is big.
     */
    get<Video.VideoStream> { it ->
        val video = daoCache.videoById(it.id)

        if (video == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val type = ContentType.fromFilePath(video.videoFileName).first {
                it.contentType == "video"
            }
            call.respond(LocalFileContent(File(video.videoFileName), contentType = type))
        }
    }
}
