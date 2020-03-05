/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import com.icerockdev.common.database.sql.statements.InsertOrUpdateStatement
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <T : Table> T.insertOrUpdate(key: Column<*>, body: T.(InsertStatement<Number>) -> Unit): Int {
    val query = InsertOrUpdateStatement<Number>(key, this)
    body(query)
    return query.execute(TransactionManager.current())!!
}
