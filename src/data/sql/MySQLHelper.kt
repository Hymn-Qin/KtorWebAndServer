package com.geely.gic.hmi.data.sql

import com.geely.gic.hmi.utils.toJsonArray
import org.slf4j.LoggerFactory
import java.sql.DriverManager

class MySQLHelper {
    private val logger by lazy { LoggerFactory.getLogger(MySQLHelper::class.java) }
    companion object {
        const val JDBC_DRIVER = "com.mysql.jdbc.Driver"
        const val DB_URL = "jdbc:mysql://localhost:3306/mydb?useUnicode=true&characterEncoding=UTF-8"
        const val DB_USER = "root"
        const val DB_PASSWORD = "123456"
    }

    fun init () {

    }

    fun getUser(): String {
        Class.forName(JDBC_DRIVER)
        val conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
        val stmt = conn.prepareStatement("SELECT * FROM MyTable WHERE id = ?")
        stmt.setInt(1, 10000)
        val rs = stmt.executeQuery()
        val jsonStr = rs.toJsonArray()
//        while (rs.next()) {
//            val msg = rs.getString(rs.findColumn("name"))
//
//        }
        logger.info("查询数据：{}", jsonStr)
        rs.close()
        stmt.close()
        conn.close()
        return jsonStr
    }

    fun insertUser(username: String) {
        Class.forName(JDBC_DRIVER)
        val conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
        val stmt = conn.prepareStatement("INSERT INTO MyTable(user) VALUES (?)")
        stmt.setString(1, username)
        stmt.executeUpdate()
        stmt.close()
        conn.close()
    }
}