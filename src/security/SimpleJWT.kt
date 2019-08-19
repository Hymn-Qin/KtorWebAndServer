package com.geely.gic.hmi.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm

open class SimpleJWT(secret: String) {

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm).build()

    fun sign(name: String): String =
        JWT.create().withClaim("name", name).sign(algorithm)
}