package web

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.model.Session
import com.geely.gic.hmi.utils.redirect
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.locations.*
import io.ktor.request.receive

/**
 * Register the [UserPage] route '/user/{user}',
 * with the user profile.
 */
fun Route.userPage(dao: DAOFacade) {
    /**
     * A GET request will return a page with the profile of a given user from its [UserPage.user] name.
     * If the user doesn't exists, it will return a 404 page instead.
     */
    get<Users.UserPage> {
        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
        val pageUser = dao.user(it.userId)

        if (pageUser == null) {
            call.respond(HttpStatusCode.NotFound.description("User ${it.userId} doesn't exist"))
        } else {
            val kweets = dao.userKweets(it.userId).map { dao.getKweet(it) }
            val etag = (user?.userId ?: "") + "_" + kweets.map { it.text.hashCode() }.hashCode().toString()

            call.respond(FreeMarkerContent("user.ftl", mapOf("user" to user, "pageUser" to pageUser, "kweets" to kweets), etag))
        }
    }

    post<Users.UserPage> {
        val post = call.receive<Parameters>()

        val userId = post["userId"] ?: return@post call.redirect(it.copy(error = "Invalid userId"))
        val password = post["password"] ?: return@post call.redirect(it.copy(error = "Invalid password"))
        val newPassword = post["newPassword"]
        val email = post["email"]
        val displayName = post["displayName"]

        val error = Users.UserPage(userId = userId)

//        when {
//            newPassword != null && newPassword.length < 6 -> return@post call.redirect(error.copy(error = "NewPassword should be at least 6 characters long"))
//            email != null && dao.userByEmail(email) != null -> return@post call.redirect(error.copy(error = "User with the following email $email is already registered"))
//            else -> {
//                val user = dao.user(userId)
//                    ?: return@post call.redirect(error.copy(error = "User with the following login is already registered"))
//
//                val hash = if (newPassword != null) hashFunction(newPassword) else hashFunction(password)
//
//                val newUser = User(
//                    userId = userId,
//                    email = email ?: user.email,
//                    displayName = displayName ?: user.displayName,
//                    passwordHash = hash
//                )
//                try {
//                    dao.updateUser(newUser)
//                } catch (e: Throwable) {
//                    application.log.error("Failed to update user", e)
//                    return@post call.redirect(error.copy(error = "Failed to update"))
//                }
//
//                val result = "OK"
//                call.respond(result)
//            }
//        }
    }
}
