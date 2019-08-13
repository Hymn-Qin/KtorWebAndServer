package com.geely.gic.hmi.http

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.model.User
import com.geely.gic.hmi.http.security.*
import io.ktor.application.*
import io.ktor.http.Parameters
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

fun Route.register(dao: DAOFacade) {
    post<Users.Register> {
        val post = call.receive<Parameters>()
        logger.info("REGISTER POST: {}", post)
        val userId = post["userId"] ?: return@post call.redirect(it)
        val password = post["password"] ?: return@post call.redirect(it)
        val email = post["email"] ?: return@post call.redirect(it)
        val displayName = post["displayName"] ?: return@post call.redirect(it)

        val error = Users.Register(userId = userId, displayName = displayName, email = email)

        when {
            password.length < 6 -> call.redirect(error.copy(error = "Password should be at least 6 characters long"))
            userId.length < 4 -> call.redirect(error.copy(error = "Login should be at least 4 characters long"))
            !userNameValid(userId) -> call.redirect(error.copy(error = "Login should be consists of digits, letters, dots or underscores"))
            dao.user(userId) != null -> call.redirect(error.copy(error = "User with the following login is already registered"))
            else -> {
                val hash = hashFunction(password)
                val newUser = User(userId = userId, email = email, displayName = displayName, passwordHash = hash)
                try {
                    dao.createUser(newUser)
                } catch (e: Throwable) {
                    when {
                        // NOTE: This is security issue that allows to enumerate/verify registered users. Do not do this in real app :)
                        dao.user(userId) != null -> call.redirect(error.copy(error = "User with the following login is already registered"))
                        dao.userByEmail(email) != null -> call.redirect(error.copy(error = "User with the following email $email is already registered"))
                        else -> {
                            application.log.error("Failed to register user", e)
                            call.redirect(error.copy(error = "Failed to register"))
                        }
                    }
                }

                val result = mapOf("msg" to "OK")
                call.respond(result)
            }
        }
    }
}