package web

import com.geely.gic.hmi.Index
import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.model.InvalidAccountException
import com.geely.gic.hmi.data.model.Reply
import com.geely.gic.hmi.data.model.Session
import com.geely.gic.hmi.data.model.User
import com.geely.gic.hmi.security.userNameValid
import com.geely.gic.hmi.utils.address
import com.geely.gic.hmi.utils.redirect
import com.geely.gic.hmi.utils.respondDefaultHtml
import com.geely.gic.hmi.utils.respondRedirect
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.locations.url
import io.ktor.request.receive
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.coroutines.async
import kotlinx.html.*

/**
 * Registers the [Users.Login] and [Users.Logout] routes '/login' and '/logout'.
 */
fun Route.login(dao: DAOFacade, client: HttpClient, hash: (String) -> String) {
    /**
     * A GET request to the [Users.Login], would respond with the login page
     * (unless the user is already logged in, in which case it would redirect to the user's page)
     */
//    get<Users.Login> {
//        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
//
//        if (user != null) {
//            call.redirect(Users.UserPage(user.userId))
//        } else {
//            call.respond(FreeMarkerContent("login.ftl",
//                mapOf("userId" to it.userId, "error" to it.error),
//                "")
//            )
//        }
//    }


    get<Users.Login> {

        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
        application.log.info("Login GET session:{}", user)

        if (user != null) {
            call.redirect(Users.UserPage(user.userId))
        } else {
            call.sessions.clear<Session>()
            call.respondDefaultHtml(emptyList(), CacheControl.Visibility.Public) {
                h2 { +"Login" }
                form(
                    call.url(Users.Login()) { parameters.clear() },
                    classes = "pure-form-stacked",
                    encType = FormEncType.applicationXWwwFormUrlEncoded,
                    method = FormMethod.post
                ) {
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
                    if (it.error != null) {
                        div() {
                            +"error:${it.error}"
                        }
                    }
                }
            }
        }

    }

    /**
     * A POST request to the [Users.Login] actually processes the [Parameters] to validate them, if valid it sets the session.
     * It will redirect either to the [Users.Login] page with an error in the case of error,
     * or to the [Users.UserPage] if the login was successful.
     */
    post<Users.Login> {

        val post = call.receive<Parameters>()
        val userId = post["userId"] ?: return@post
        val password = post["password"] ?: return@post

        val error = Users.Login(userId)

        val login = when {
            userId.length < 4 -> null
            password.length < 6 -> null
            !userNameValid(userId) -> null
            else -> {
                //如果前后端分离的话 在这做登录
                val user = async {
                    val data = client.get<Reply<User>>(call.address(Users.Login(userId = userId, password = password))) {
                    }
                    application.log.info("Login POST and Client get Api userId = $userId, reply data:{}", data)
                    if (data.code == HttpStatusCode.OK.value) {
                            data.result
                        } else {
                            call.redirect(error.copy(error = data.msg))
                            throw InvalidAccountException(data.toString())
                        }
                }
                user.await()
                //查询是否有该用户，并且帐号密码正确
//                dao.user(userId, hash(password))
            }

        }

        if (login == null) {
            call.redirect(error.copy(error = "Invalid username or password"))
        } else {
            //返回 session
            call.sessions.set(Session(login.userId))
            //重定向到用户详情页
            call.redirect(Users.UserPage(login.userId))
        }
    }

    /**
     * A GET request to the [Users.Logout] page, removes the session and redirects to the [Index] page.
     */
    get<Users.Logout> {
        //清理session
        call.sessions.clear<Session>()
        //重定向到首页
        call.redirect(Index())
    }
}
