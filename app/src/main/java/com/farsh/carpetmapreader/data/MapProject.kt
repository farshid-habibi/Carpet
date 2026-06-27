package com.farsh.carpetmapreader.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class MapProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUri: String?,
    val rows: Int,
    val cols: Int,
    val direction: String = "RTL", // "LTR", "RTL", "ZIGZAG"
    val speed: Float = 1.0f,
    val pauseBetweenCells: Long = 1000, // milliseconds
    val pauseBetweenRows: Long = 2000,  // milliseconds
    val currentRow: Int = 0,
    val currentCol: Int = 0,
    val mapType: String = "GRID", // "GRID" or "NUMERICAL"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "map_cells",
    foreignKeys = [
        ForeignKey(
            entity = MapProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class MapCell(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val rowIdx: Int,
    val colIdx: Int,
    val number: String?, // Recognized text/digits
    val colorHex: String, // Hex string like "#FF0000"
    val colorName: String, // Nearest Persian color name e.g. "قرمز"
    val isRead: Boolean = false,
    val sectionName: String = "LEFT"
)
