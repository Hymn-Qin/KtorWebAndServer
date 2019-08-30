package com.geely.gic.hmi.web

import com.geely.gic.hmi.Index
import com.geely.gic.hmi.Video
import com.geely.gic.hmi.data.dao.VideoDatabase
import com.geely.gic.hmi.utils.respondDefaultHtml
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import java.io.*

/**
 * Register video-related routes: [Index] (/), [Video.VideoPage] (/video/page/{id}) and [Video.VideoStream] (/video/{id})
 */
fun Route.videos(videoDatabase: VideoDatabase) {
    /**
     * The index route that doesn't have any parameters, returns a HTML with a list of videos linking to their pages
     * and displayed unlinked author names.
     */
    get<Index> {
        //        val session = call.sessions.get<YouKubeSession>()
        val userId = "123"//session.userId
        val topVideos = videoDatabase.top()
        val etag = topVideos.joinToString { "${it.id},${it.title}" }.hashCode().toString() + "-" + userId?.hashCode()
        val visibility = if (userId == null) CacheControl.Visibility.Public else CacheControl.Visibility.Private

        call.respondDefaultHtml(listOf(EntityTagVersion(etag)), visibility) {
            div("posts") {
                when {
                    topVideos.isEmpty() -> {
                        h1("content-subhead") { +"No Videos" }
                        div {
                            +"You need to upload some videos to watch them"
                        }
                    }
                    topVideos.size < 11 -> {
                        h1("content-subhead") { +"Videos" }
                    }
                    else -> {
                        h1("content-subhead") { +"Top 10 Videos" }
                    }
                }
                topVideos.forEach {
                    section("post") {
                        header("post-header") {
                            h3("post-title") {
                                a(href = locations.href(Video.VideoPage(it.authorId, it.id))) { +it.title }
                            }
                            p("post-meta") {
                                +"by ${it.authorId}"
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The [Video.VideoPage] returns an HTML with the information about a specified video by [Video.VideoPage.id]
     * including the video itself, being streamed by the [Video.VideoStream] route.
     * If the video doens't exists, responds with a 404 [HttpStatusCode.NotFound].
     */
    get<Video.VideoPage> {
        val video = videoDatabase.videoById(it.id)

        if (video == null) {
            call.respond(HttpStatusCode.NotFound.description("Video ${it.id} doesn't exist"))
        } else {
            call.respondDefaultHtml(
                listOf(EntityTagVersion(video.hashCode().toString())),
                CacheControl.Visibility.Public
            ) {

                section("post") {
                    header("post-header") {
                        h3("post-title") {
                            a(href = locations.href(Video.VideoPage(it.authorId, it.id))) { +video.title }
                        }
                        p("post-meta") {
                            +"by ${video.authorId}"
                        }
                    }
                }

                video("pure-u-5-5") {
                    controls = true
                    source {
                        src = call.url(Video.VideoStream(it.id))
                        type = "video/ogg"
                    }
                }
            }
        }
    }

    /**
     * Returns the bits of the video specified by [Video.VideoStream.id] or [HttpStatusCode.NotFound] if the video is not found.
     * It returns a [LocalFileContent] that works along the installed [PartialContent] feature to support getting chunks
     * of the content, and allowing the navigator to seek the video even if the video content is big.
     */
    get<Video.VideoStream> { it ->
        val video = videoDatabase.videoById(it.id)

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
