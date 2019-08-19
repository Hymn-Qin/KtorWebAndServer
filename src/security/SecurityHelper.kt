package com.geely.gic.hmi.security

import io.ktor.util.hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Hardcoded secret hash key used to hash the passwords, and to authenticate the sessions.
 */
val hashKey = hex("6819b57a326945c1968f45236589")
/**
 * HMac SHA1 key spec for the password hashing.
 */
val hmacKey = SecretKeySpec(hashKey, "HmacSHA1")

/**
 * 密码加密
 *
 * Method that hashes a [password] by using the globally defined secret key [hmacKey].
 */
private fun hash(password: String): String {
    val hmac = Mac.getInstance("HmacSHA1")
    hmac.init(hmacKey)
    return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
}

// Provides a hash function to be used when registering the resources.
val hashFunction = { s: String -> hash(s) }
/**
 * Pattern to validate an `userId`
 */
private val userIdPattern = "[a-zA-Z0-9_\\.]+".toRegex()

/**
 * Validates that an [userId] (that is also the user name) is a valid identifier.
 * Here we could add additional checks like the length of the user.
 * Or other things like a bad word filter.
 */
internal fun userNameValid(userId: String) = userId.matches(userIdPattern)