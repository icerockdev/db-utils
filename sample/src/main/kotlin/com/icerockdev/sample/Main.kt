/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.sample

import com.icerockdev.common.database.ConnectionManager
import com.icerockdev.common.database.DBConfig
import org.jetbrains.exposed.sql.Database
import kotlin.system.exitProcess


object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val connectionManager = ConnectionManager(
            DBConfig(
                driverClassName = "org.postgresql.Driver",
                jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/db",
                username = "user",
                password = "pass",
                schema = "public",
                maxPoolSize = 5
            ).apply {
                connectionInitSql = "set time zone UTC"
                idleTimeout = 600000
            }
        )

        // await db is ready
        connectionManager.awaitReady()

        // checking db isReady
        if (!connectionManager.isReady()) {
            println("DB is not ready")
            exitProcess(1)
        }

        // exposed connection
        Database.connect(connectionManager.getDataSource())
    }
}
