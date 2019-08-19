package com.geely.gic.hmi.data.dao

import com.mchange.v2.c3p0.ComboPooledDataSource
import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import org.h2.Driver
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.io.IOException

fun Application.userDao(path: String): DAOFacade {

    // First we initialize the database.
    /**
     * File where the database is going to be stored.
     */
    val dir = File("path")

    /**
     * Pool of JDBC connections used.
     */
    val pool = ComboPooledDataSource().apply {
        driverClass = Driver::class.java.name
        jdbcUrl = "jdbc:h2:file:${dir.canonicalFile.absolutePath}"
        user = ""
        password = ""
    }
    /**
     * Constructs a facade with the database, connected to the DataSource configured earlier with the [dir]
     * for storing the database.
     */
    val dao: DAOFacade = DAOFacadeCache(DAOFacadeDatabase(Database.connect(pool)), File(dir.parentFile, "ehcache"))
    dao.init()
    // And we subscribe to the stop event of the application, so we can also close the [ComboPooledDataSource] [pool].
    environment.monitor.subscribe(ApplicationStopped) { pool.close() }
    return dao
}


fun Application.videoDao(uploadDirPath: String): DAOVideoCache {
    val uploadDir = File(uploadDirPath)
    if (!uploadDir.mkdirs() && !uploadDir.exists()) {
        throw IOException("Failed to create directory ${uploadDir.absolutePath}")
    }
    val dao = DAOVideoCache(uploadDir)
    return dao
}