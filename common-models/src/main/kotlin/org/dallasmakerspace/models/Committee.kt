package org.dallasmakerspace.models

import kotlinx.serialization.Serializable

@Serializable
data class Committee(
    val id: Int,
    val name: String,
    val chairGroupName: String?,
    val description: String? = null,
    val color: String? = null,
    val isActive: Boolean = true,
    val teacherGroups: List<String> = emptyList(),
    val groupPrefixes: List<String> = emptyList(),
)

object Committees {
  val ALL =
      listOf(
          Committee(
              id = 1,
              name = "3D Fab",
              chairGroupName = "3D Fabrication Chair",
              teacherGroups = listOf("3D Printer Teachers", "3D Vacuum Former Teachers"),
              groupPrefixes = listOf("3D"),
              isActive = true,
          ),
          Committee(
              id = 2,
              name = "Blacksmith",
              chairGroupName = "Blacksmith Committee Chair",
              teacherGroups = listOf("Blacksmith Teachers"),
              groupPrefixes = listOf("Blacksmith"),
              isActive = true,
          ),
          Committee(
              id = 3,
              name = "Creative Arts",
              chairGroupName = "Creative Arts Committee Chair",
              teacherGroups = listOf("CA Teachers"),
              groupPrefixes = listOf("CA"),
              isActive = true,
          ),
          Committee(
              id = 4,
              name = "Ceramics",
              chairGroupName = "Fired Arts Committee Chair",
              teacherGroups = listOf("Ceramics Teachers"),
              groupPrefixes = listOf("Ceramics"),
              isActive = true,
          ),
          Committee(
              id = 5,
              name = "Digital Media",
              chairGroupName = "Digital Media Committee Chair",
              teacherGroups = listOf("Digital Media Darkroom Teachers", "Digital Media Teachers"),
              groupPrefixes = listOf("Digital Media"),
              isActive = true,
          ),
          Committee(
              id = 6,
              name = "Electronics and Robotics",
              chairGroupName = "Electronics and Robotics Committee Chair",
              teacherGroups = listOf("Electronics Teachers"),
              groupPrefixes = listOf("Electronics"),
              isActive = true,
          ),
          Committee(
              id = 7,
              name = "Glassworks",
              chairGroupName = "Glassworks Committee Chair",
              teacherGroups = listOf("Glassworks Teachers"),
              groupPrefixes = listOf("Glassworks"),
              isActive = true,
          ),
          Committee(
              id = 8,
              name = "Jewelry",
              chairGroupName = "Jewelry Committee Chair",
              teacherGroups = listOf("Jewelry Casting Teachers", "Lapidary Teachers"),
              groupPrefixes = listOf("Jewelry", "Lapidary"),
              isActive = true,
          ),
          Committee(
              id = 9,
              name = "Laser",
              chairGroupName = "Laser Committee Chair",
              teacherGroups = listOf("Laser Teachers"),
              groupPrefixes = listOf("Laser"),
              isActive = true,
          ),
          Committee(
              id = 11,
              name = "Machine Shop",
              chairGroupName = "Machine Shop Committee Chair",
              teacherGroups =
                  listOf(
                      "Machine Shop Haas Teachers",
                      "Machine Shop Nomad Teachers",
                      "Machine Shop Shapeoko Teachers",
                      "Machine Shop Teachers",
                  ),
              groupPrefixes = listOf("Machine Shop"),
              isActive = true,
          ),
          Committee(
              id = 11,
              name = "Metal Shop",
              chairGroupName = "Metal Shop Committee Chair",
              teacherGroups = listOf("Metal Shop Teachers"),
              groupPrefixes = listOf("Metal Shop"),
              isActive = true,
          ),
          Committee(
              id = 12,
              name = "Science",
              chairGroupName = "Science Committee Chair",
              teacherGroups = listOf("Science Teachers"),
              groupPrefixes = listOf("Science"),
              isActive = true,
          ),
          Committee(
              id = 13,
              name = "Woodshop",
              chairGroupName = "Woodshop Committee Chair",
              teacherGroups =
                  listOf("WS Shapeoko Teachers", "Woodshop Multicam Teachers", "Woodshop Teachers"),
              groupPrefixes = listOf("Woodshop", "WS"),
              isActive = true,
          ),
      )

  fun findById(id: Int): Committee? = ALL.find { it.id == id }

  fun findByName(name: String): Committee? = ALL.find { it.name.equals(name, ignoreCase = true) }
}
