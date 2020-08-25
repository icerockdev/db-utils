package com.icerockdev.common.database

import ca.krasnay.sqlbuilder.ParameterizedPreparedStatementCreator
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

fun prepare(connection: Connection, sql: String, params: List<Any>): PreparedStatement {
    return connection.prepareStatement(sql).let { psc ->
        params.forEachIndexed { i, param ->
            val index = i + 1
            psc.setObject(index, param)
        }
        psc
    }
}

fun prepareParameterized(connection: Connection, sql: String, params: Map<String, Any>): PreparedStatement {
    return ParameterizedPreparedStatementCreator()
        .setSql(sql).let { psc ->
            params.forEach { param ->
                psc.setParameter(param.key, param.value)
            }
            psc.createPreparedStatement(connection)
        }
}

fun <T : Any> PreparedStatement.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = arrayListOf<T>()
    var rs: ResultSet? = null
    try {
        rs = this.executeQuery()
        while (rs.next()) {
            result += transform(rs)
        }
        return result
    } finally {
        rs?.close()
        this.close()
    }
}

fun <T : Any> PreparedStatement.findOne(transform: (ResultSet) -> T): T? {
    var rs: ResultSet? = null
    try {
        rs = this.executeQuery()
        if (rs.next()) {
            return transform(rs)
        }
        return null
    } finally {
        rs?.close()
        this.close()
    }
}

fun PreparedStatement.update(): Int {
    this.use {
        return this.executeUpdate()
    }
}
