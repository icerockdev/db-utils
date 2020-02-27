/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.Time

fun Table.point(name: String, length: Int = 4326): Column<GeographyPoint> =
    registerColumn(name, GeographyPointColumnType(length))

fun Table.time(name: String): Column<Time> =
    registerColumn(name, TimeColumnType())


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

fun <T : Table> T.insertOrUpdate(key: Column<*>, body: T.(InsertStatement<Number>) -> Unit): Int {
    val query = InsertOrUpdate<Number>(key, this)
    body(query)
    return query.execute(TransactionManager.current())!!
}

// postgres only!
class InsertOrUpdate<Key : Any>(private val key: Column<*>,
                                table: Table,
                                isIgnore: Boolean = false) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val updateSetter = table.columns.joinToString { "${it.name} = EXCLUDED.${it.name}" }
        val onConflict = "ON CONFLICT (${key.name}) DO UPDATE SET $updateSetter"
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}

fun <T : Any> Table.jsonb(name: String, klass: TypeReference<T>, jsonMapper: ObjectMapper): Column<T>
        = registerColumn(name, Json(klass.type::class.java, jsonMapper))

fun <T : Any> Table.jsonb(name: String, klass: Class<T>, jsonMapper: ObjectMapper): Column<T>
        = registerColumn(name, Json(klass, jsonMapper))

private class Json<out T : Any>(private val klass: Class<T>, private val jsonMapper: ObjectMapper) : ColumnType() {
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
            jsonMapper.readValue(value.value, klass)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Can't parse JSON: $value")
        }
    }

    override fun notNullValueToDB(value: Any): Any = jsonMapper.writeValueAsString(value)
    override fun nonNullValueToString(value: Any): String = "'${jsonMapper.writeValueAsString(value)}'"
}
