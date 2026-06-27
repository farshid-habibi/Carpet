package com.farsh.carpetmapreader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.farsh.carpetmapreader.data.MapCell
import com.farsh.carpetmapreader.data.MapProject
import com.farsh.carpetmapreader.data.MapRepository
import com.farsh.carpetmapreader.processor.CarpetReaderEngine
import com.farsh.carpetmapreader.processor.ImageGridDetector
import com.farsh.carpetmapreader.processor.OCRProcessor
import com.farsh.carpetmapreader.processor.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CarpetViewModel(
    private val context: Context,
    private val repository: MapRepository
) : ViewModel() {

    private val gridDetector = ImageGridDetector()
    private val ocrProcessor = OCRProcessor()
    
    // Lazy instance of TTS and player engine
    val ttsManager = TextToSpeechManager(context)
    val readerEngine = CarpetReaderEngine(context, repository, ttsManager)

    private val _projects = MutableStateFlow<List<MapProject>>(emptyList())
    val projects: StateFlow<List<MapProject>> = _projects.asStateFlow()

    private val _activeProject = MutableStateFlow<MapProject?>(null)
    val activeProject: StateFlow<MapProject?> = _activeProject.asStateFlow()

    private val _activeCells = MutableStateFlow<List<MapCell>>(emptyList())
    val activeCells: StateFlow<List<MapCell>> = _activeCells.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Expose engine play state
    val playState = readerEngine.playState
    val currentHighlightCell = readerEngine.currentCell

    init {
        // Collect all historical projects from Repository
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                _projects.value = list
            }
        }
        // Sync engine's live cell updates to activeCells for reactive UI
        viewModelScope.launch {
            readerEngine.currentCells.collect { cells ->
                if (cells.isNotEmpty()) {
                    _activeCells.value = cells
                }
            }
        }
    }

    /**
     * Load an already created project and bind it to Carpet player engine
     */
    fun selectProject(projectId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Collect cells and project flow
            val proj = repository.getProjectByIdDirect(projectId)
            if (proj != null) {
                _activeProject.value = proj
                val cells = repository.getCellsForProjectDirect(projectId)
                _activeCells.value = cells
                readerEngine.setupProject(proj, cells)
            } else {
                _errorMessage.value = "پروژه یافت نشد"
            }
            _isLoading.value = false
        }
    }

    /**
     * Delete active project
     */
    fun deleteProject(project: MapProject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(project)
            // If the deleted project was active, clear states
            if (_activeProject.value?.id == project.id) {
                withContext(Dispatchers.Main) {
                    _activeProject.value = null
                    _activeCells.value = emptyList()
                    readerEngine.stop()
                }
            }
        }
    }

    /**
     * Triggers async image processor to slice the carpet map, compute color features,
     * execute OCR concurrently on all cells, and write output to SQLite.
     * Supports both "GRID" and "NUMERICAL" map types.
     */
    fun createProjectFromImage(
        bitmap: Bitmap,
        name: String,
        rows: Int,
        cols: Int,
        direction: String,
        mapType: String = "GRID"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Pre-validations for warnings
                if (bitmap.width < 100 || bitmap.height < 100) {
                    _errorMessage.value = "ابعاد عکس بسیار کوچک است. لطفاً عکس باکیفیت‌تری بگیرید."
                    _isLoading.value = false
                    return@launch
                }

                // 1. Persist the image file locally in filesDir
                val imagePath = withContext(Dispatchers.IO) {
                    val directory = File(context.filesDir, "carpet_maps")
                    if (!directory.exists()) directory.mkdirs()
                    val file = File(directory, "carpet_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                // 2. Insert blank project placeholder in database to obtain project autoGeneratedValue ID
                val newProject = MapProject(
                    name = name,
                    imageUri = imagePath,
                    rows = rows,
                    cols = cols,
                    direction = direction,
                    mapType = mapType
                )
                val projectId = withContext(Dispatchers.IO) {
                    repository.insertProject(newProject)
                }

                val finalCells = if (mapType == "NUMERICAL") {
                    _errorMessage.value = "در حال خواندن جدول عددی با هوش مصنوعی..."
                    val parser = com.farsh.carpetmapreader.processor.NumericalMapParser()
                    withContext(Dispatchers.Default) {
                        parser.parseFromImage(bitmap, projectId)
                    }
                } else {
                    // 3. Segment map into grid coordinates
                    val processedCells = withContext(Dispatchers.Default) {
                        gridDetector.detectGrid(bitmap, rows, cols, projectId)
                    }

                    if (processedCells.isEmpty()) {
                        _errorMessage.value = "خطا در تقسیم‌بندی جدول نقشه"
                        _isLoading.value = false
                        return@launch
                    }

                    // 4. Concurrently analyze each cell via ML Kit OCR
                    _errorMessage.value = "در حال پردازش خانه‌ها و خواندن اعداد با هوش مصنوعی..."
                    withContext(Dispatchers.Default) {
                        processedCells.map { processed ->
                            val text = ocrProcessor.recognizeText(processed.cellBitmap)
                            processed.cell.copy(number = text)
                        }
                    }
                }

                // 5. Bulk insert processed cells into Database
                withContext(Dispatchers.IO) {
                    repository.insertCells(finalCells)
                }

                // 6. Automatically select newly created project
                selectProject(projectId)
                
            } catch (e: Exception) {
                Log.e("CarpetViewModel", "Error creating carpet project", e)
                _errorMessage.value = "خطا در پردازش نقشه فرش: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load Bitmap from private local URI
     */
    fun loadProjectBitmap(uriString: String?): Bitmap? {
        if (uriString.isNullOrEmpty()) return null
        return try {
            val file = File(uriString)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CarpetViewModel", "Failed to decode project bitmap", e)
            null
        }
    }

    /**
     * Manual Override Hook: Let users adjust recognized cell digits or colors
     */
    fun updateCellManual(cell: MapCell, newNumber: String?, newColorName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = cell.copy(number = newNumber, colorName = newColorName)
            repository.updateCell(updated)
            
            // Refresh local active values
            val currentList = _activeCells.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == cell.id }
            if (index != -1) {
                currentList[index] = updated
                _activeCells.value = currentList
                
                withContext(Dispatchers.Main) {
                    // Reinitialize engine without losing current pointer
                    activeProject.value?.let { proj ->
                        readerEngine.setupProject(proj, currentList)
                    }
                    
                    // Highlight manual update directly
                    readerEngine.jumpToCell(updated)
                }
            }
        }
    }

    /**
     * Update settings parameter live
     */
    fun updateProjectSettings(speed: Float, pauseCells: Long, pauseRows: Long, direction: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = proj.copy(
                speed = speed,
                pauseBetweenCells = pauseCells,
                pauseBetweenRows = pauseRows,
                direction = direction
            )
            repository.updateProject(updated)
            withContext(Dispatchers.Main) {
                _activeProject.value = updated
                readerEngine.setupProject(updated, _activeCells.value)
            }
        }
    }

    /**
     * Reset complete read status progress
     */
    fun resetProjectProgress() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllCellsReadState(proj.id)
            val updatedProject = proj.copy(currentRow = 0, currentCol = 0)
            repository.updateProject(updatedProject)
            
            // Reload
            val cells = repository.getCellsForProjectDirect(proj.id)
            withContext(Dispatchers.Main) {
                _activeProject.value = updatedProject
                _activeCells.value = cells
                readerEngine.setupProject(updatedProject, cells)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        readerEngine.release()
        ttsManager.shutdown()
    }
}

/**
 * Custom Factory injection to provide application context
 */
class CarpetViewModelFactory(
    private val context: Context,
    private val repository: MapRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarpetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarpetViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
