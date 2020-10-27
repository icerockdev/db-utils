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
import kotlin.reflect.KClass

class GeographyPointColumnType(private val length: Int = 4326) : ColumnType() {
    override fun sqlType(): String = "geography(Point, $length)"

    override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "geography"
        obj.value = value?.toString()
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
open class PGEnumColumnType<T : Enum<T>>(
    private val enumName: String,
    private val klass: KClass<T>,
    private val enumType: String? = null
) : ColumnType() {
    override fun sqlType(): String = enumType ?: "${enumName}_enum"
    override fun valueFromDB(value: Any): Any = when (value) {
        is String -> klass.java.enumConstants!!.first { it.name == value }
        is Enum<*> -> value
        else -> error("$value of ${value::class.qualifiedName} is not valid for enum ${klass.qualifiedName}")
    }

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Enum<*> -> PGEnum(sqlType(), value as T)
        else -> error("$value of ${value::class.qualifiedName} is not valid for enum ${klass.qualifiedName}")
    }
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
