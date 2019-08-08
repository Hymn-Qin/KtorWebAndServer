package com.geely.gic.hmi.route

import com.geely.gic.hmi.User
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post

fun Route.user() {
    post<User.UserLogin> {
        call.respondText("登录成功")
    }

    post<User.UserRegister> { user ->
        run {
            call.respondText("用户${user.username}${user.password}")
        }
    }
}