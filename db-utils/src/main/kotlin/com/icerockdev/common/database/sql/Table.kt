/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.Time

/**
 * A geography point column
 *
 * @param name The column name
 * @param length The column length
 */
fun Table.point(name: String, length: Int = 4326): Column<GeographyPoint> =
    registerColumn(name, GeographyPointColumnType(length))

/**
 * A PostgreSQL enum column
 *
 * @param name The column name
 * @param enumType The column type (default: "${name}_enum")
 */
inline fun <reified T : Enum<T>> Table.postgresEnum(name: String, enumType: String? = null): Column<T> {
    return registerColumn(name, PGEnumColumnType(name, T::class, enumType))
}

/**
 * A time column
 *
 * @param name The column name
 */
fun Table.time(name: String): Column<Time> =
    registerColumn(name, TimeColumnType())

/**
 * A jsonb column with TypeReference
 *
 * @param name The column name
 * @param typeReference The type reference
 * @param jsonMapper Jackson object mapper
 */
fun <T : Any> Table.jsonb(name: String, typeReference: TypeReference<T>, jsonMapper: ObjectMapper): Column<T> {
    return registerColumn(name, Json(typeReference, jsonMapper))
}

/**
 * A jsonb column with Class
 *
 * @param name The column name
 * @param jsonMapper Jackson object mapper
 */
inline fun <reified T : Any> Table.jsonb(name: String, jsonMapper: ObjectMapper): Column<T> =
    registerColumn(name, Json(object : TypeReference<T>() {}, jsonMapper))

data class GeographyPoint(
    val lat: Double,
    val lng: Double
) {
    companion object {
        fun getInstance(point: String): GeographyPoint {
            val parts = point
                .split("POINT(", " ", ")")
                .filter { s -> s.isNotEmpty() }
            return GeographyPoint(parts[1].toDouble(), parts[0].toDouble())
        }
    }

    override fun toString(): String {
        return "POINT($lng $lat)"
    }
}

open class Json<out T : Any>(private val typeReference: TypeReference<T>, private val jsonMapper: ObjectMapper) : ColumnType() {

    @Deprecated("Since 0.2.0, use primary constructor.")
    constructor(clazz: Class<T>, jsonMapper: ObjectMapper) : this(object : TypeReference<T>() {}, jsonMapper)

    override fun sqlType() = "jsonb"

    override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "jsonb"
        obj.value = value as String?
        stmt.setObject(index, obj)
    }

    override fun valueFromDB(value: Any): Any {
        if (value !is PGobject) {
            // We didn't receive a PGobject (the format of stuff actually coming from the DB).
            // In that case "value" should already be an object of type T.
            return value
        }

        // We received a PGobject, deserialize its String value.
        return try {
            jsonMapper.readValue(value.value, typeReference)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Can't parse JSON: $value")
        }
    }

    override fun notNullValueToDB(value: Any): Any = jsonMapper.writeValueAsString(value)
    override fun nonNullValueToString(value: Any): String = "'${jsonMapper.writeValueAsString(value)}'"
}
