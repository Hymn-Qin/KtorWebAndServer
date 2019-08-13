package com.geely.gic.hmi.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.sql.ResultSet

/**
 * A Gson Builder with pretty printing enabled.
 */
val gson: Gson = GsonBuilder().setPrettyPrinting().create()

fun ResultSet.toJsonArray(): String {
    var str = "["
    if (first()) {
        do {
            var item = "{"
            for (i in 0 until  metaData.columnCount) {
                item += "\"${metaData.getColumnName(i)}\": \"${getString(i)}\","
            }
            item += "},"
            str += item
        } while (next())
    }
    str = str.trimEnd(',')
    str += "]"
    return str
}

fun joinOptions(options: Collection<String>) =
    options.joinToString(prefix = "[", postfix = "]")
