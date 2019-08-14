package com.geely.gic.hmi.http

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.model.Reply
import com.geely.gic.hmi.data.model.User
import com.geely.gic.hmi.http.security.SimpleJWT
import com.geely.gic.hmi.http.security.hashFunction
import com.geely.gic.hmi.http.security.userNameValid
import com.geely.gic.hmi.http.utils.*
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.CacheControl
import io.ktor.http.Parameters
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.routing.Route
import kotlinx.coroutines.async
import kotlinx.html.*

fun Route.login(simpleJwt: SimpleJWT, dao: DAOFacade, httpClient: HttpClient) {

    get<Users.Login> {
        call.respondDefaultHtml(emptyList(), CacheControl.Visibility.Public) {
            h2 { +"Login" }
            form(call.url(Users.Login()) { parameters.clear() }, classes = "pure-form-stacked", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                acceptCharset = "utf-8"

                label {
                    +"Username: "
                    textInput {
                        name = Users.Login::userId.name
                        value = it.userId
                    }
                }
                label {
                    +"Password: "
                    passwordInput {
                        name = Users.Login::password.name
                    }
                }
                submitInput(classes = "pure-button pure-button-primary") {
                    value = "Login"
                }
            }
        }
    }


    post<Users.Login> {
        logger.info("POST: {}", call.request(it))

        val post = call.receive<Parameters>()

        val userId = post["userId"] ?: return@post call.redirect(it.copy(error = "Invalid username"))
        val password = post["password"] ?: return@post call.redirect(it.copy(error = "Invalid password"))
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
            val userInfo = async {
                httpClient.get<Reply<User>>(call.address(Users.UserInfo(login.userId))).result
            }
            call.respond(userInfo.await())
        }
    }

    get<Users.Logout> {
        val post = call.receive<Parameters>()
        logger.info("LOGIN_OUT GET: {}", post)
        val userId = post["userId"] ?: return@get call.redirect(it)

    }
}