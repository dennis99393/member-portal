package org.dallasmakerspace.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Namespace(
    val id: Int,
    val name: String,
    val ownerType: NamespaceOwnerType,
    val ownerGroupId: Int?,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val createdBy: Int?,
    val aliases: List<NamespaceAlias> = emptyList()
)

@Serializable
enum class NamespaceOwnerType {
  COMMITTEE,
  SYSTEM
}

@Serializable
data class NamespaceAlias(
    val id: Int,
    val namespaceId: Int,
    val alias: String,
    val isPrimary: Boolean,
    val createdAt: Instant
)

@Serializable
data class ShortLink(
    val id: Int,
    val namespaceId: Int?,
    val slug: String,
    val redirectType: RedirectType,
    val destinationUrl: String,
    val description: String?,
    val creatorId: Int?,
    val updatedBy: Int?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val clickCount: Long = 0
)

@Serializable
enum class RedirectType {
  BASIC,
  DYNAMIC
}

@Serializable
data class ShortLinkClick(
    val id: Long,
    val shortLinkId: Int,
    val memberId: Int?,
    val resolvedPath: String?,
    val variableValues: String?,
    val clickedAt: Instant,
    val ipAddress: String?,
    val userAgent: String?,
    val referrer: String?
)
