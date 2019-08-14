package com.geely.gic.hmi.data.model

import com.geely.gic.hmi.utils.gson

class Reply<T: Any> (val code: Int, val msg: String, val result: T? = null) {
    override fun toString(): String {
        return gson.toJson(this)
    }
}