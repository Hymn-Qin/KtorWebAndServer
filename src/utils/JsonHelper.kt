package com.geely.gic.hmi

import java.sql.ResultSet

class JsonHelper {
}

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
