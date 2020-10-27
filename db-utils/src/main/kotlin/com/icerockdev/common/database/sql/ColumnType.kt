/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgis.PGgeometry
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.Timestamp

class GeographyPointColumnType(private val length: Int = 4326) : ColumnType() {
    override fun sqlType(): String = "geography(Point, $length)"

    override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "geography"
        obj.value = value as String?
        stmt.setObject(index, obj)
    }

    private fun getGeographyPoint(value: String): GeographyPoint {
        val geometry = PGgeometry.geomFromString(value).getPoint(0)
        return if (geometry != null) {
            GeographyPoint(geometry.y, geometry.x)
        } else {
            error("$value of ${value::class.qualifiedName} is not valid geo object")
        }
    }

    override fun valueFromDB(value: Any): GeographyPoint {
        return when (value) {
            is PGobject -> getGeographyPoint(value.value)
            is String -> getGeographyPoint(value)
            else -> error("$value of ${value::class.qualifiedName} is not valid geo object")
        }
    }
}

@Suppress("UNCHECKED_CAST")
open class EnumColumnType<T : Enum<T>>(
    private val enumName: String,
    private val clazz: Class<*>,
    private val enumType: String? = null
) : ColumnType() {
    override fun sqlType(): String = enumType ?: "${enumName}_enum"
    override fun valueFromDB(value: Any): Any {
        return java.lang.Enum.valueOf(clazz as Class<T>, value as String)
    }

    override fun notNullValueToDB(value: Any): Any = PGEnum(sqlType(), value as T)
}

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}

fun Table.timestamp(name: String): Column<Timestamp> = registerColumn(name, TimestampColumnType())

class TimestampColumnType : ColumnType() {
    override fun sqlType(): String = "TIMESTAMP"

    override fun nonNullValueToString(value: Any): String {
        return "'${value}'"
    }

    override fun valueFromDB(value: Any): Any = value

    override fun notNullValueToDB(value: Any): Any {
        return value
    }
}

class TimeColumnType : ColumnType() {
    override fun sqlType(): String = "time"
}
