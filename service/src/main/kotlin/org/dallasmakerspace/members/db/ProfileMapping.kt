package org.dallasmakerspace.members.db

import kotlinx.coroutines.Dispatchers
import org.dallasmakerspace.models.DMSMember
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/** Exposed table for the mariaDB table - profile. */
@Suppress("MagicNumber")
object ProfileTable : IdTable<Int>("profile") {
  val idColumn: Column<EntityID<Int>> = integer("id").autoIncrement().entityId()
  val username: Column<EntityID<String>> = varchar("username", 100).entityId()
  val avatarUrl: Column<String?> = varchar("avatar_url", 2083).nullable()
  val discourseUsername: Column<String?> = varchar("discourse_username", 100).nullable()
  val discourseAvatarUrl: Column<String?> = varchar("discourse_avatar_url", 2083).nullable()
  val discordUserId: Column<String?> = varchar("discord_userid", 100).nullable()
  val attributes: Column<String?> = text("attributes").nullable()
  override val id: Column<EntityID<Int>>
    get() = idColumn
}

/** Column aliases used in other tables to reference the profile table. */
object ProfileColumnAliases {
  val actorProfileAlias = ProfileTable.alias("actorProfile")
  val subjectProfileAlias = ProfileTable.alias("subjectProfile")
}

class ProfileDAO(rowId: EntityID<Int>) : Entity<Int>(rowId) {
  companion object : EntityClass<Int, ProfileDAO>(ProfileTable)

  var username by ProfileTable.username
  var avatarUrl by ProfileTable.avatarUrl
  var discourseUsername by ProfileTable.discourseUsername
  var discourseAvatarUrl by ProfileTable.discourseAvatarUrl
  var discordUserId by ProfileTable.discordUserId
  var attributes by ProfileTable.attributes
}

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun daoToProfileModel(dao: ProfileDAO) =
    DMSMember(
        dao.id.value,
        username = dao.username.value,
        avatarUrl = dao.avatarUrl,
        discourseUsername = dao.discourseUsername,
        discourseAvatarUrl = dao.discourseAvatarUrl,
        discordUserId = dao.discordUserId)
