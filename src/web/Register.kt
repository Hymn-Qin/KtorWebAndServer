package web

import com.geely.gic.hmi.Users
import com.geely.gic.hmi.data.dao.DAOFacade
import com.geely.gic.hmi.data.model.InvalidAccountException
import com.geely.gic.hmi.data.model.Reply
import com.geely.gic.hmi.data.model.Session
import com.geely.gic.hmi.data.model.User
import com.geely.gic.hmi.security.isUserEmailValid
import com.geely.gic.hmi.security.userNameValid
import com.geely.gic.hmi.utils.MultiPartContent
import com.geely.gic.hmi.utils.redirect
import com.geely.gic.hmi.utils.respondDefaultHtml
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.locations.url
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.coroutines.async
import kotlinx.html.*

/**
 * Register routes for user registration in the [Users.Register] route (/register)
 */
fun Route.register(dao: DAOFacade, client: HttpClient, hash: (String) -> String) {

    /**
     * A GET request would show the registration form (with an error if specified by the URL in the case there was an error in the form processing)
     * If the user is already logged, it redirects the client to the [Users.UserPage] instead.
     */
    get<Users.Register> {
        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
        application.log.info("Register GET session:{}", user)
        if (user != null) {
            call.redirect(Users.UserPage(user.userId))
        } else {
//            call.respond(
//                FreeMarkerContent(
//                    "register.ftl", mapOf(
//                        "pageUser" to User(
//                            it.userId,
//                            it.email,
//                            it.displayName,
//                            ""
//                        ), "error" to it.error
//                    ), ""
//                )
//            )
            call.sessions.clear<Session>()
            call.respondDefaultHtml(emptyList(), CacheControl.Visibility.Public) {
                h2 { +"Register" }
                form(
                    call.url(Users.Register()) { parameters.clear() },
                    classes = "pure-form-stacked",
                    encType = FormEncType.applicationXWwwFormUrlEncoded,
                    method = FormMethod.post
                ) {
                    acceptCharset = "utf-8"

                    label {
                        +"Username: "
                        textInput {
                            name = Users.Register::userId.name
                            value = it.userId
                        }
                    }
                    label {
                        +"Email: "
                        textInput {
                            name = Users.Register::email.name
                            value = it.email
                        }
                    }
                    label {
                        +"DisplayName: "
                        textInput {
                            name = Users.Register::displayName.name
                            value = it.displayName
                        }
                    }
                    label {
                        +"Password: "
                        passwordInput {
                            name = Users.Register::password.name
                        }
                    }
                    submitInput(classes = "pure-button pure-button-primary") {
                        value = "Register"
                    }
                    if (it.error != null) {
                        div {
                            +it.error
                        }
                    }

                }
            }
        }
    }


    /**
     * A POST request to the [Users.Register] route, will try to create a new user.
     *
     * - If the user is already logged, it redirects to the [Users.UserPage] page.
     * - If not specified the userId, password, displayName or email, it will redirect to the [Users.Register] form page.
     * - Then it will validate the specified parameters, redirecting displaying an error to the [Users.Register] page.
     * - On success, it generates a new [User]. But instead of storing the password plain text,
     *   it stores a hash of the password.
     */
    post<Users.Register> {
        // get current from session data if any
        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
        application.log.info("Register POST session:{}", user)
        // user already logged in? redirect to user page.
        if (user != null) return@post call.redirect(Users.UserPage(user.userId))

        // receive post data
        // TODO: use conneg when it's ready and `call.receive<Register>()`
        val post = call.receive<Parameters>()
        val userId = post["userId"] ?: return@post call.redirect(it.copy(error = "Invalid userId"))
        val password = post["password"] ?: return@post call.redirect(it.copy(error = "Invalid password"))
        val displayName = post["displayName"] ?: return@post call.redirect(it.copy(error = "Invalid displayName"))
        val email = post["email"] ?: return@post call.redirect(it.copy(error = "Invalid email"))

        // prepare location class for error if any
        val error = Users.Register(userId, displayName, email)

        when {
            password.length < 6 -> call.redirect(error.copy(error = "Password should be at least 6 characters long"))
            userId.length < 4 -> call.redirect(error.copy(error = "Login should be at least 4 characters long"))
            !isUserEmailValid(email) -> call.redirect(error.copy(error = "Invalid email"))
            !userNameValid(userId) -> call.redirect(error.copy(error = "Login should be consists of digits, letters, dots or underscores"))
//            dao.user(userId) != null -> call.redirect(error.copy(error = "User with the following login is already registered"))
            else -> {
//                val hash = hash(password)
//                val newUser = User(userId, email, displayName, hash)
//                try {
//                    dao.createUser(newUser)
//                } catch (e: Throwable) {
//                    when {
//                        // NOTE: This is security issue that allows to enumerate/verify registered users. Do not do this in real app :)
//                        dao.user(userId) != null -> call.redirect(error.copy(error = "User with the following login is already registered"))
//                        dao.userByEmail(email) != null -> call.redirect(error.copy(type = "email", error = "User with the following email $email is already registered"))
//                        else -> {
//                            application.log.error("Failed to register user", e)
//                            call.redirect(error.copy(error = "Failed to register"))
//                        }
//                    }
//                }
                val hash = hash(password)
                val newUser = User(userId, email, displayName, hash)
                val code = async {
                    val data = client.post<Reply<String>>("http://127.0.0.1:8088/user/register") {
                        body = MultiPartContent.build {
                            add(Users.Register::userId.name, userId)
                            add(Users.Register::email.name, email)
                            add(Users.Register::displayName.name, displayName)
                            add(Users.Register::password.name, password)
//                            add("file", byteArrayOf(1, 2, 3, 4), filename = "binary.bin")
                        }
                    }
                    application.log.info("Register POST and Client post Api userId = $userId, reply data:{}", data)
                    if (data.code == HttpStatusCode.OK.value) {
                        true
                    } else {
                        call.redirect(error.copy(error = data.msg))
                        throw InvalidAccountException(data.toString())
                    }
                }
                if (code.await()) {
                    call.sessions.set(Session(newUser.userId))
                    call.redirect(Users.UserPage(newUser.userId))
                }
            }
        }
    }
}

