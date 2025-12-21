package org.dallasmakerspace.routing

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Resource("/short-links")
class ShortLinksResource {

  @Resource("namespaces")
  class Namespaces(val parent: ShortLinksResource = ShortLinksResource()) {

    @Resource("create") class Create(val parent: Namespaces = Namespaces())

    @Resource("{id}")
    class ById(val parent: Namespaces = Namespaces(), val id: Int) {

      @Resource("update") class Update(val parent: ById)

      @Resource("delete") class Delete(val parent: ById)

      @Resource("aliases")
      class Aliases(val parent: ById) {

        @Resource("add") class Add(val parent: Aliases)

        @Resource("{aliasId}/delete") class DeleteAlias(val parent: Aliases, val aliasId: Int)
      }
    }
  }

  @Resource("links")
  class Links(val parent: ShortLinksResource = ShortLinksResource()) {

    @Resource("create") class Create(val parent: Links = Links())

    @Resource("{id}")
    class ById(val parent: Links = Links(), val id: Int) {

      @Resource("update") class Update(val parent: ById)

      @Resource("delete") class Delete(val parent: ById)
    }

    @Resource("by-namespace/{namespaceId}")
    class ByNamespace(val parent: Links = Links(), val namespaceId: Int)
  }
}

// Request/Response DTOs

@Serializable
data class CreateNamespaceRequest(
    val name: String,
    val ownerType: String,
    val ownerGroupId: Int?,
    val description: String?,
    val primaryAlias: String,
    val additionalAliases: List<String> = emptyList()
)

@Serializable
data class UpdateNamespaceRequest(
    val name: String,
    val description: String?,
    val isActive: Boolean
)

@Serializable data class AddAliasRequest(val alias: String, val isPrimary: Boolean = false)

@Serializable
data class CreateShortLinkRequest(
    val namespaceId: Int?,
    val slug: String?,
    val destinationUrl: String,
    val description: String?
)

@Serializable
data class UpdateShortLinkRequest(
    val slug: String,
    val destinationUrl: String,
    val description: String?,
    val isActive: Boolean
)

@Serializable
data class PopularShortLinkResponse(
    val link: org.dallasmakerspace.models.ShortLink,
    val clicks: Long
)
