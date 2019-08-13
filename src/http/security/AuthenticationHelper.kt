package com.geely.gic.hmi.http.security

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.jwt.jwt

private val simpleJWT = SimpleJWT("my-super-secret-for-jwt")

fun Application.authentication(): SimpleJWT {
    //身份认证特性
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

        //JWT令牌取代基本的basic身份认证  安装并配置 JWT
        jwt {
            verifier(simpleJWT.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }
    return simpleJWT
}