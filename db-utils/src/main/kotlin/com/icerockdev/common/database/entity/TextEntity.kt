/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.common.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.Column

open class TextIdTable(name: String = "", columnName: String = "id") : IdTable<String>(name) {
    override val id: Column<EntityID<String>> = text(columnName).primaryKey().entityId()
}
abstract class TextEntity(id: EntityID<String>) : Entity<String>(id)

abstract class TextEntityClass<out E:TextEntity>(table: IdTable<String>, entityType: Class<E>? = null) : EntityClass<String, E>(table, entityType)
