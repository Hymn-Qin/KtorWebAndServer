package com.geely.gic.hmi.utils

import com.geely.gic.hmi.data.model.Session
import io.ktor.application.ApplicationCall
import io.ktor.sessions.sessions

//校验session是否过期
fun ApplicationCall.expiration(): Boolean {
    var ret = true
//    val s = sessions.get("Session") as? Session
//    if (s != null && System.currentTimeMillis() < s.expiration + 3 * 60 * 1000) {
//        ret = false
//    }
    return ret
}