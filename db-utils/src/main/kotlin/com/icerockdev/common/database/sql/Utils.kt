/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.slf4j.LoggerFactory

fun getSortOrder(orderByString: String?): SortOrder = when (orderByString) {
    "ASC" -> SortOrder.ASC
    else -> SortOrder.DESC
}

object KotlinLoggingSqlLogger : SqlLogger {
    private val logger = LoggerFactory.getLogger(KotlinLoggingSqlLogger::class.java)
    override fun log(context: StatementContext, transaction: Transaction) {
        logger.info("SQL: ${context.expandArgs(transaction)}")
    }
}