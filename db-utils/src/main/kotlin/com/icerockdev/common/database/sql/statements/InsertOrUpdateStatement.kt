/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql.statements

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement

/**
 * Only for PostgreSQL
 */
open class InsertOrUpdateStatement<Key : Any>(private val key: Column<*>,
                                              table: Table,
                                              isIgnore: Boolean = false) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val updateSetter = table.columns.joinToString { "${it.name} = EXCLUDED.${it.name}" }
        val onConflict = "ON CONFLICT (${key.name}) DO UPDATE SET $updateSetter"
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}
