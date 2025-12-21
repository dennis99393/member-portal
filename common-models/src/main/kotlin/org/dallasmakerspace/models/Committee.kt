package org.dallasmakerspace.models

import kotlinx.serialization.Serializable

@Serializable
data class Committee(
    val id: Int,
    val name: String,
    val chairGroupName: String?,
    val description: String? = null,
    val color: String,
    val isActive: Boolean = true
)

object Committees {
  val ALL =
      listOf(
          Committee(
              id = 1,
              name = "Woodshop",
              chairGroupName = "Woodshop Officers",
              description = "Wood working and carpentry",
              color = "#8B4513",
              isActive = true),
          Committee(
              id = 2,
              name = "Ceramics",
              chairGroupName = "Ceramics Committee",
              description = "Pottery and ceramic arts",
              color = "#CD853F",
              isActive = true),
          Committee(
              id = 3,
              name = "Automotive",
              chairGroupName = "Automotive Committee",
              description = "Vehicle maintenance and repair",
              color = "#FF4500",
              isActive = true))

  fun findById(id: Int): Committee? = ALL.find { it.id == id }

  fun findByName(name: String): Committee? = ALL.find { it.name.equals(name, ignoreCase = true) }
}
