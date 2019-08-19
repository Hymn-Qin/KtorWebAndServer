package com.geely.gic.hmi.security

import com.geely.gic.hmi.Users
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.locations.url

private val simpleJWT = SimpleJWT("my-super-secret-for-jwt")

fun Application.authentication(users: UserHashedTableAuth): SimpleJWT {
    //身份认证特性  token
    install(Authentication) {
        //基本的身份认证
//        basic {
//            realm = "myrealm"
//            validate {
//                if (it.name == "user" && it.password == "password")
//                    UserIdPrincipal("user")
//                else
//                    null
//            }
//        }


//        val myFormAuthentication = "myFormAuthentication"
//        form(myFormAuthentication) {
//            userParamName = Users.Login::userId.name
//            passwordParamName = Users.Login::password.name
//            challenge = FormAuthChallenge.Redirect {
//                url(Users.Login(it?.name ?: ""))
//            }
//            validate { users.authenticate(it) }
//        }

        //JWT令牌取代基本的basic身份认证  安装并配置 JWT  token
        jwt {
            verifier(simpleJWT.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }
    return simpleJWT
}