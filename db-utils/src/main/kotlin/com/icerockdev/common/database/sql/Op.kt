/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.sql

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function

fun Column<GeographyPoint>.pointAs(column: String): Function<String> {
    return GeogAsText(this, column)
}

fun Column<GeographyPoint>.dWithin(point: GeographyPoint, column: Column<Int>): Op<Boolean> {
    return DWithin(this, point, column)
}

private class GeogAsText(val expr: ExpressionWithColumnType<*>, val column: String) :
    Function<String>(GeographyPointColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("ST_AsText(", (expr as Column).name, ")", " AS ", column)
    }
}

private class DWithin(val expr: ExpressionWithColumnType<*>, val point: GeographyPoint, val column: Column<Int>) :
    Op<Boolean>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(
            "ST_DWithin(",
            (expr as Column).name,
            ", ST_GeogFromText('", point.toString(), "') ,",
            column.name,
            ")"
        )
    }
}

fun <T : Any> geogFromText(point: GeographyPoint) = object : Expression<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("ST_GeogFromText('", point.toString(), "')")
    }
}

/**
 * @see <a href="https://www.postgresql.org/docs/current/pgtrgm.html">PostgreSQL Documentation</a>
 */
infix fun <T, S1 : T?, S2 : T?> Expression<in S1>.distance(other: Expression<in S2>): Op<Boolean> =
    DistanceOp(this, other)

class DistanceOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "<->")

class ILikeOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "ILIKE")

infix fun<T:String?> ExpressionWithColumnType<T>.ilike(pattern: String): Op<Boolean> = ILikeOp(this, QueryParameter(pattern, columnType))
