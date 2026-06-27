package com.farsh.carpetmapreader.data

import kotlinx.coroutines.flow.Flow

class MapRepository(private val mapDao: MapDao) {

    val allProjects: Flow<List<MapProject>> = mapDao.getAllProjects()

    fun getProjectById(id: Long): Flow<MapProject?> {
        return mapDao.getProjectById(id)
    }

    suspend fun getProjectByIdDirect(id: Long): MapProject? {
        return mapDao.getProjectByIdDirect(id)
    }

    fun getCellsForProject(projectId: Long): Flow<List<MapCell>> {
        return mapDao.getCellsForProject(projectId)
    }

    suspend fun getCellsForProjectDirect(projectId: Long): List<MapCell> {
        return mapDao.getCellsForProjectDirect(projectId)
    }

    suspend fun insertProject(project: MapProject): Long {
        return mapDao.insertProject(project)
    }

    suspend fun updateProject(project: MapProject) {
        mapDao.updateProject(project)
    }

    suspend fun deleteProject(project: MapProject) {
        mapDao.deleteProject(project)
    }

    suspend fun insertCells(cells: List<MapCell>) {
        mapDao.insertCells(cells)
    }

    suspend fun updateCell(cell: MapCell) {
        mapDao.updateCell(cell)
    }

    suspend fun updateCellReadStatus(cellId: Long, isRead: Boolean) {
        mapDao.updateCellReadStatus(cellId, isRead)
    }

    suspend fun resetAllCellsReadState(projectId: Long) {
        mapDao.resetAllCellsReadState(projectId)
    }
}
