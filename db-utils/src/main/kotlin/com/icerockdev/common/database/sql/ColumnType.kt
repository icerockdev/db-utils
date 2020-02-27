/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.Timestamp

class GeographyPointColumnType(private val length: Int = 4326) : ColumnType() {
    override fun sqlType(): String = "geography(Point, $length)"

    override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "geography"
        obj.value = value.toString()
        stmt.setObject(index, obj)
    }
}

fun Table.timestamp(name: String): Column<Timestamp> = registerColumn(name, TimestampColumnType())

class TimestampColumnType(): ColumnType() {
    override fun sqlType(): String = "TIMESTAMP"

    override fun nonNullValueToString(value: Any): String {
        return "'${value}'"
    }

    override fun valueFromDB(value: Any): Any = value

    override fun notNullValueToDB(value: Any): Any {
        return value
    }
}

class TimeColumnType() : ColumnType() {
    override fun sqlType(): String = "time"
}