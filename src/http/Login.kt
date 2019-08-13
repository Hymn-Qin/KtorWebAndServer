package com.geely.gic.hmi.http

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.http.security.*
import io.ktor.application.call
import io.ktor.http.Parameters
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

fun Route.login(simpleJwt: SimpleJWT, dao: DAOFacade) {
    post<Users.Login> {

        val post = call.receive<Parameters>()
        logger.info("LOGIN POST: {}", post)
        val userId = post["userId"] ?: return@post call.redirect(it)
        val password = post["password"] ?: return@post call.redirect(it)
        val error = Users.Login(userId)
        val login = when {
            userId.length < 4 -> null
            password.length < 6 -> null
            !userNameValid(userId) -> null
            //查询数据库
            else -> dao.user(userId, hashFunction(password))
        }

        if (login == null) {
            call.redirect(error.copy(error = "Invalid username or password"))
        } else {
            //生成令牌
            val token = simpleJwt.sign(login.userId)
            //返回json数据
            val result = mapOf("msg" to "OK")
            call.respond(result)
        }
    }

    get<Users.Logout> {
        val post = call.receive<Parameters>()
        logger.info("LOGIN OUT GET: {}", post)
        val userId = post["userId"] ?: return@get call.redirect(it)

    }
}