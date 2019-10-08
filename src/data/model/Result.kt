package com.geely.gic.hmi.data.model

import com.geely.gic.hmi.utils.gson

class Result<T: Any> (val code: Int, val msg: String, val result: T? = null) {
    override fun toString(): String {
        return gson.toJson(this)
    }
}

//data class HttpBinError(
//        val request: String,
//        val message: String,
//        val code: HttpStatusCode,
//        val cause: Throwable? = null
//)