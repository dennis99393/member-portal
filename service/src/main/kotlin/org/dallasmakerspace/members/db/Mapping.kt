package org.dallasmakerspace.members.db

import kotlinx.coroutines.Dispatchers
import org.dallasmakerspace.models.DMSMember
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/** Exposed table for the mariaDB table - profile. */
@Suppress("MagicNumber")
object ProfileTable : IdTable<String>("profile") {
  val username: Column<EntityID<String>> = varchar("username", 100).entityId()
  val avatarUrl: Column<String?> = varchar("avatar_url", 2083).nullable()
  val discourseUsername: Column<String?> = varchar("discourse_username", 100).nullable()
  val discourseAvatarUrl: Column<String?> = varchar("discourse_avatar_url", 2083).nullable()
  val attributes: Column<String?> = text("attributes").nullable()
  override val id: Column<EntityID<String>>
    get() = username
}

class ProfileDAO(username: EntityID<String>) : Entity<String>(username) {
  companion object : EntityClass<String, ProfileDAO>(ProfileTable)

  var avatarUrl by ProfileTable.avatarUrl
  var discourseUsername by ProfileTable.discourseUsername
  var discourseAvatarUrl by ProfileTable.discourseAvatarUrl
  var attributes by ProfileTable.attributes
}

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun daoToModel(dao: ProfileDAO) =
    DMSMember(dao.id.value, dao.avatarUrl, dao.discourseUsername, dao.discourseAvatarUrl, null)
