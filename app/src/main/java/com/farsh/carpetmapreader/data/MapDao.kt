package com.farsh.carpetmapreader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MapDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<MapProject>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<MapProject?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdDirect(id: Long): MapProject?

    @Query("SELECT * FROM map_cells WHERE projectId = :projectId ORDER BY id ASC")
    fun getCellsForProject(projectId: Long): Flow<List<MapCell>>

    @Query("SELECT * FROM map_cells WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getCellsForProjectDirect(projectId: Long): List<MapCell>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: MapProject): Long

    @Update
    suspend fun updateProject(project: MapProject)

    @Delete
    suspend fun deleteProject(project: MapProject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<MapCell>)

    @Update
    suspend fun updateCell(cell: MapCell)

    @Query("UPDATE map_cells SET isRead = :isRead WHERE id = :cellId")
    suspend fun updateCellReadStatus(cellId: Long, isRead: Boolean)

    @Query("UPDATE map_cells SET isRead = 0 WHERE projectId = :projectId")
    suspend fun resetAllCellsReadState(projectId: Long)

    @Query("SELECT COUNT(*) FROM map_cells WHERE projectId = :projectId")
    suspend fun getCellCountForProject(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM map_cells WHERE projectId = :projectId AND isRead = 1")
    suspend fun getReadCellCountForProject(projectId: Long): Int
}
