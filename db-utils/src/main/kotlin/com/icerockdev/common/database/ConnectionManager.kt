/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource


class ConnectionManager(config: DBConfig) {
    private val logger = LoggerFactory.getLogger(ConnectionManager::class.java)
    private var dataSource: HikariDataSource

    init {
        val driver = config.driverClassName
        Class.forName(driver)

        val hikariConfig = HikariConfig()
        hikariConfig.driverClassName = config.driverClassName
        hikariConfig.jdbcUrl = config.jdbcUrl
        hikariConfig.username = config.username
        hikariConfig.password = config.password
        hikariConfig.connectionInitSql = config.connectionInitSql
        hikariConfig.connectionTimeout = 2 * 1000L
        hikariConfig.idleTimeout = config.idleTimeout
        hikariConfig.schema = config.schema
        hikariConfig.initializationFailTimeout = 5 * 1000L
        hikariConfig.minimumIdle = 1

        val maxPoolSize = config.maxPoolSize
        if (maxPoolSize > 0) {
            hikariConfig.maximumPoolSize = maxPoolSize
        }

        dataSource = HikariDataSource(hikariConfig)
    }

    fun isReady(): Boolean {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            if (connection.prepareStatement("SELECT 1").execute()) {
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        } finally {
            connection?.close()
        }
    }

    fun awaitReady() {
        logger.info("Await DB connection.")
        while (true) {
            if (isReady()) {
                logger.info("Connection DB ready.")
                return
            }
            Thread.sleep(1000L)
        }
    }

    fun getDataSource(): DataSource {
        return dataSource
    }

    fun close() {
        dataSource.close()
    }
}

data class DBConfig(
    val driverClassName: String,
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val schema: String,
    val maxPoolSize: Int,
    var connectionInitSql: String = "set time zone 'UTC'",
    var idleTimeout: Long = 600000
)