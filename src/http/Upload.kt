package com.geely.gic.hmi.http

import com.geely.gic.hmi.Video
import com.geely.gic.hmi.http.data.Database
import com.geely.gic.hmi.http.utils.respondDefaultHtml
import com.geely.gic.hmi.http.utils.respondRedirect
import com.geely.gic.hmi.utils.copyToSuspend
import io.ktor.application.call
import io.ktor.http.CacheControl
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.locations.url
import io.ktor.request.receiveMultipart
import io.ktor.routing.Route
import kotlinx.html.*
import java.io.File

/**
 * Register [Upload] routes.
 */
fun Route.upload(database: Database, uploadDir: File) {
    get<Video.Upload> {
//        val session = call.sessions.get<YouKubeSession>()
//        if (session == null) {
//            call.respondRedirect(Login())
//        } else {
            call.respondDefaultHtml(emptyList(), CacheControl.Visibility.Private) {
                h2 { +"Upload video" }

                form(
                    call.url(Video.Upload()),
                    classes = "pure-form-stacked",
                    encType = FormEncType.multipartFormData,
                    method = FormMethod.post
                ) {
                    acceptCharset = "utf-8"

                    label {
                        htmlFor = "title"; +"Title:"
                        textInput { name = "title"; id = "title" }
                    }

                    br()
                    fileInput { name = "file" }
                    br()

                    submitInput(classes = "pure-button pure-button-primary") { value = "Upload" }
                }
            }
//        }
    }


    /**
     * Registers a POST route for [Upload] that actually read the bits sent from the client and creates a new video
     * using the [database] and the [uploadDir].
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
                        uploadDir,
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
        val id = database.addVideo(title, userId, videoFile!!)
        call.respondRedirect(Video.VideoPage(userId, id))

//        }
    }
}