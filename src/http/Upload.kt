package com.geely.gic.hmi.http

import com.geely.gic.hmi.Video
import com.geely.gic.hmi.data.dao.DAOVideoCache
import com.geely.gic.hmi.utils.respondRedirect
import com.geely.gic.hmi.utils.copyToSuspend
import io.ktor.application.call
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.locations.post
import io.ktor.request.receiveMultipart
import io.ktor.routing.Route
import java.io.File

/**
 * Register [Upload] routes.
 */
fun Route.upload(daoCache: DAOVideoCache) {
    /**
     * Registers a POST route for [Upload] that actually read the bits sent from the client and creates a new video
     * using the [daoCache] and the [daoCache.uploadDir].
     */
    post<Video.Upload> {

        //        val session = call.sessions.get<YouKubeSession>()

//        if (session == null) {
//            call.respond(HttpStatusCode.Forbidden.description("Not logged in"))
//
//        } else {
        val multipart = call.receiveMultipart()
        var title = ""
        var videoFile: File? = null
        val userId = "123"//session.userId
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> if (part.name == "title") {
                    title = part.value
                }
                is PartData.FileItem -> {
                    val ext = File(part.originalFileName).extension
                    val file = File(
                        daoCache.uploadDir,
                        "upload-${System.currentTimeMillis()}-${userId.hashCode()}-${title.hashCode()}.$ext"
                    )

                    part.streamProvider().use { its ->
                        file.outputStream().buffered().use {
                            its.copyToSuspend(it)
                        }
                    }
                    videoFile = file
                }
            }
            part.dispose()
        }
        val id = daoCache.addVideo(title, userId, videoFile!!)
        call.respondRedirect(Video.VideoPage(userId, id))

//        }
    }
}