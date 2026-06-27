package com.farsh.carpetmapreader.processor

import android.content.Context
import android.util.Log
import com.farsh.carpetmapreader.data.MapCell
import com.farsh.carpetmapreader.data.MapProject
import com.farsh.carpetmapreader.data.MapRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarpetReaderEngine(
    private val context: Context,
    private val repository: MapRepository,
    private val ttsManager: TextToSpeechManager
) {
    enum class PlayState {
        IDLE, PLAYING, PAUSED, FINISHED
    }

    private val _playState = MutableStateFlow(PlayState.IDLE)
    val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentCell = MutableStateFlow<MapCell?>(null)
    val currentCell: StateFlow<MapCell?> = _currentCell.asStateFlow()

    private val _currentCells = MutableStateFlow<List<MapCell>>(emptyList())
    val currentCells: StateFlow<List<MapCell>> = _currentCells.asStateFlow()

    private var activeProject: MapProject? = null
    private var cellList: List<MapCell> = emptyList()
    
    private var currentIndex = 0
    private var engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Tie TTS completion to automatically advance to next cell
        ttsManager.setSpeechDoneListener { utteranceId ->
            if (utteranceId == "cell_read" && _playState.value == PlayState.PLAYING) {
                advanceAfterDelay()
            }
        }
    }

    fun setupProject(project: MapProject, cells: List<MapCell>) {
        activeProject = project
        cellList = cells
        _currentCells.value = cells
        _playState.value = PlayState.IDLE
        
        // Find saved coordinates index, default to top-left or top-right depending on layout direction
        val savedIndex = cells.indexOfFirst { 
            it.rowIdx == project.currentRow && it.colIdx == project.currentCol 
        }
        
        currentIndex = if (savedIndex != -1) savedIndex else 0
        if (cellList.isNotEmpty()) {
            _currentCell.value = cellList[currentIndex]
        }
    }

    fun start() {
        if (cellList.isEmpty() || activeProject == null) return
        _playState.value = PlayState.PLAYING
        speakCurrentCell()
    }

    fun pause() {
        Log.d("CarpetReaderEngine", "Pausing engine")
        _playState.value = PlayState.PAUSED
        ttsManager.stop()
        engineScope.coroutineContext.cancelChildren() // Cancel any pending timers
        saveReadingProgress()
    }

    fun stop() {
        Log.d("CarpetReaderEngine", "Stopping engine")
        _playState.value = PlayState.IDLE
        ttsManager.stop()
        engineScope.coroutineContext.cancelChildren()
        saveReadingProgress()
    }

    fun jumpToCell(row: Int, col: Int) {
        val index = cellList.indexOfFirst { it.rowIdx == row && it.colIdx == col }
        if (index != -1) {
            engineScope.coroutineContext.cancelChildren() // Cancel pending speech advances
            currentIndex = index
            val cell = cellList[currentIndex]
            _currentCell.value = cell
            saveReadingProgress()
            
            if (_playState.value == PlayState.PLAYING) {
                speakCurrentCell()
            }
        }
    }

    fun jumpToCell(cell: MapCell) {
        val index = cellList.indexOfFirst { it.id == cell.id }
        val targetIndex = if (index != -1) index else {
            cellList.indexOfFirst { it.rowIdx == cell.rowIdx && it.colIdx == cell.colIdx }
        }
        if (targetIndex != -1) {
            engineScope.coroutineContext.cancelChildren() // Cancel pending speech advances
            currentIndex = targetIndex
            val foundCell = cellList[currentIndex]
            _currentCell.value = foundCell
            saveReadingProgress()
            
            if (_playState.value == PlayState.PLAYING) {
                speakCurrentCell()
            }
        }
    }

    fun next() {
        engineScope.coroutineContext.cancelChildren()
        val nextIdx = calculateNextIndex(currentIndex)
        if (nextIdx != null) {
            currentIndex = nextIdx
            _currentCell.value = cellList[currentIndex]
            saveReadingProgress()
            if (_playState.value == PlayState.PLAYING) {
                speakCurrentCell()
            }
        } else {
            finishMap()
        }
    }

    fun previous() {
        engineScope.coroutineContext.cancelChildren()
        val prevIdx = calculatePreviousIndex(currentIndex)
        if (prevIdx != null) {
            currentIndex = prevIdx
            _currentCell.value = cellList[currentIndex]
            saveReadingProgress()
            if (_playState.value == PlayState.PLAYING) {
                speakCurrentCell()
            }
        }
    }

    private fun speakCurrentCell() {
        val cell = _currentCell.value ?: return
        val project = activeProject ?: return

        // Format conversational Persian TTS instructions
        val textToSpeak = if (project.mapType == "NUMERICAL") {
            val isNewSection = currentIndex == 0 || cellList[currentIndex - 1].sectionName != cell.sectionName
            val isNewColor = isNewSection || cellList[currentIndex - 1].rowIdx != cell.rowIdx
            
            val sectionSpeech = if (isNewSection) {
                when (cell.sectionName) {
                    "LEFT" -> "ستون سمت چپ، "
                    "RIGHT" -> "ستون سمت راست، "
                    "BOTTOM" -> "بخش پایین نقشه، "
                    else -> ""
                }
            } else {
                ""
            }

            val colorFarsiName = cell.colorName.ifEmpty { "" }
            val colorCodeWord = cell.rowIdx.toString()
            val numStr = cell.number ?: ""
            val farsiNum = numStr.replace("-", " تا ")

            if (isNewColor) {
                val colorPart = if (colorFarsiName.isNotEmpty()) "رنگ $colorFarsiName" else ""
                val codePart = "کد رنگ $colorCodeWord"
                val intro = if (colorPart.isNotEmpty()) "$codePart، $colorPart" else codePart
                "${sectionSpeech}$intro، گره $farsiNum"
            } else {
                "گره $farsiNum"
            }
        } else {
            val persianRow = (cell.rowIdx + 1).toString()
            val persianCol = (cell.colIdx + 1).toString()
            val colorPart = if (cell.colorName.isNotEmpty()) "، رنگ ${cell.colorName}" else ""
            val numPart = if (!cell.number.isNullOrEmpty()) "، عدد ${cell.number}" else ""
            "ردیف $persianRow، ستون $persianCol$numPart$colorPart"
        }

        // Mark cell as read in the database
        val updatedCell = cell.copy(isRead = true)
        GlobalScope.launch(Dispatchers.IO) {
            repository.updateCell(updatedCell)
        }
        // Update local memory list and emit for reactive UI
        val mutable = cellList.toMutableList()
        val idx = mutable.indexOfFirst { it.id == cell.id }
        if (idx != -1) {
            mutable[idx] = updatedCell
            cellList = mutable
            _currentCells.value = mutable
        }

        // Output audio sound
        Log.d("CarpetReaderEngine", "Speaking: $textToSpeak")
        val success = ttsManager.speak(textToSpeak, "cell_read", speed = project.speed)
        if (!success) {
            Log.w("CarpetReaderEngine", "TTS speak failed or not initialized. Advancing automatically via standard delay.")
            advanceAfterDelay()
        }
    }

    private fun advanceAfterDelay() {
        val project = activeProject ?: return
        val current = _currentCell.value ?: return
        
        engineScope.launch {
            // Determine if we finished the row
            val nextIdx = calculateNextIndex(currentIndex)
            
            if (nextIdx != null) {
                val nextCell = cellList[nextIdx]
                val delayTime = if (nextCell.rowIdx != current.rowIdx || nextCell.sectionName != current.sectionName) {
                    if (project.mapType == "NUMERICAL") {
                        val nextColorCode = nextCell.rowIdx.toString()
                        val colorFarsiName = nextCell.colorName
                        val colorIntro = if (colorFarsiName.isNotEmpty()) "کد رنگ $nextColorCode، رنگ $colorFarsiName" else "کد رنگ $nextColorCode"
                        
                        val sectionChangeText = if (nextCell.sectionName != current.sectionName) {
                            val secText = when (nextCell.sectionName) {
                                "LEFT" -> "ستون سمت چپ، "
                                "RIGHT" -> "ستون سمت راست، "
                                "BOTTOM" -> "بخش پایین نقشه، "
                                else -> ""
                            }
                            "تغییر بخش به $secText"
                        } else {
                            ""
                        }
                        
                        ttsManager.speak("${sectionChangeText}رنگ بعدی، $colorIntro", "color_barrier", project.speed)
                    } else {
                        speakEndOfRowBarrier(project.speed)
                    }
                    project.pauseBetweenRows
                } else {
                    project.pauseBetweenCells
                }
                
                delay(delayTime)
                
                // If user didn't pause while waiting, execute next
                if (_playState.value == PlayState.PLAYING) {
                    withContext(Dispatchers.Main) {
                        currentIndex = nextIdx
                        _currentCell.value = cellList[currentIndex]
                        saveReadingProgress()
                        speakCurrentCell()
                    }
                }
            } else {
                speakEndOfMapBarrier(project.speed)
                delay(project.pauseBetweenRows)
                withContext(Dispatchers.Main) {
                    finishMap()
                }
            }
        }
    }

    private fun speakEndOfRowBarrier(speed: Float) {
        ttsManager.speak("پایان ردیف", "row_barrier", speed)
    }

    private fun speakEndOfMapBarrier(speed: Float) {
        ttsManager.speak("پایان نقشه", "map_barrier", speed)
    }

    private fun finishMap() {
        _playState.value = PlayState.FINISHED
        saveReadingProgress()
    }

    private fun saveReadingProgress() {
        val cell = _currentCell.value ?: return
        val project = activeProject ?: return
        GlobalScope.launch(Dispatchers.IO) {
            repository.updateProject(
                project.copy(
                    currentRow = cell.rowIdx,
                    currentCol = cell.colIdx
                )
            )
        }
    }

    /**
     * Calculates next coordinate index depending on weaving pattern:
     * - RTL (Right to Left): read cols backwards (cols-1, cols-2... 0), then jump next row.
     * - LTR (Left to Right): read cols forward (0, 1... cols-1), then jump next row.
     * - ZIGZAG (زیگزاگ): row 0 goes RTL, row 1 goes LTR, row 2 RTL, alternating.
     */
    private fun calculateNextIndex(fromIndex: Int): Int? {
        val project = activeProject ?: return null
        if (cellList.isEmpty()) return null

        if (project.mapType == "NUMERICAL") {
            return if (fromIndex < cellList.size - 1) fromIndex + 1 else null
        }

        val totalRows = project.rows
        val totalCols = project.cols

        val currentCell = cellList[fromIndex]
        val r = currentCell.rowIdx
        val c = currentCell.colIdx

        val isZigzagRtlRow = project.direction == "ZIGZAG" && r % 2 == 0

        val (nextR, nextC) = when (project.direction) {
            "LTR" -> {
                if (c < totalCols - 1) {
                    Pair(r, c + 1)
                } else if (r < totalRows - 1) {
                    Pair(r + 1, 0)
                } else {
                    return null // All finished
                }
            }
            "RTL" -> {
                if (c > 0) {
                    Pair(r, c - 1)
                } else if (r < totalRows - 1) {
                    Pair(r + 1, totalCols - 1)
                } else {
                    return null // All finished
                }
            }
            "ZIGZAG" -> {
                if (isZigzagRtlRow) {
                    // RTL part of zigzag
                    if (c > 0) {
                        Pair(r, c - 1)
                    } else if (r < totalRows - 1) {
                        // Move down to LTR row, start at col 0
                        Pair(r + 1, 0)
                    } else {
                        return null
                    }
                } else {
                    // LTR part of zigzag
                    if (c < totalCols - 1) {
                        Pair(r, c + 1)
                    } else if (r < totalRows - 1) {
                        // Move down to RTL row, start at col cols-1
                        Pair(r + 1, totalCols - 1)
                    } else {
                        return null
                    }
                }
            }
            else -> Pair(r, c + 1)
        }

        // Return real index matching nextR and nextC
        val search = cellList.indexOfFirst { it.rowIdx == nextR && it.colIdx == nextC }
        return if (search != -1) search else null
    }

    private fun calculatePreviousIndex(fromIndex: Int): Int? {
        val project = activeProject ?: return null
        if (cellList.isEmpty()) return null

        if (project.mapType == "NUMERICAL") {
            return if (fromIndex > 0) fromIndex - 1 else null
        }

        val totalCols = project.cols

        val currentCell = cellList[fromIndex]
        val r = currentCell.rowIdx
        val c = currentCell.colIdx

        val isZigzagRtlRow = project.direction == "ZIGZAG" && r % 2 == 0

        val (prevR, prevC) = when (project.direction) {
            "LTR" -> {
                if (c > 0) {
                    Pair(r, c - 1)
                } else if (r > 0) {
                    Pair(r - 1, totalCols - 1)
                } else {
                    return null // Already at start
                }
            }
            "RTL" -> {
                if (c < totalCols - 1) {
                    Pair(r, c + 1)
                } else if (r > 0) {
                    Pair(r - 1, 0)
                } else {
                    return null // Already at start
                }
            }
            "ZIGZAG" -> {
                if (isZigzagRtlRow) {
                    // RTL Row (coming from index c + 1)
                    if (c < totalCols - 1) {
                        Pair(r, c + 1)
                    } else if (r > 0) {
                        // Go back to LTR row, its end was cols-1
                        Pair(r - 1, totalCols - 1)
                    } else {
                        return null
                    }
                } else {
                    // LTR Row (coming from index c - 1)
                    if (c > 0) {
                        Pair(r, c - 1)
                    } else if (r > 0) {
                        // Go back to RTL row, its end was 0
                        Pair(r - 1, 0)
                    } else {
                        return null
                    }
                }
            }
            else -> Pair(r, c - 1)
        }

        val search = cellList.indexOfFirst { it.rowIdx == prevR && it.colIdx == prevC }
        return if (search != -1) search else null
    }

    private fun convertNumberToPersianWords(num: Int): String {
        val farsiDigits = arrayOf("صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
            "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده", "بیست")
        val farsiTens = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
        val farsiHundreds = arrayOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
        
        return when {
            num < 0 -> ""
            num <= 20 -> farsiDigits[num]
            num < 100 -> {
                val tens = num / 10
                val ones = num % 10
                if (ones == 0) farsiTens[tens] else "${farsiTens[tens]} و ${farsiDigits[ones]}"
            }
            num < 1000 -> {
                val hundreds = num / 100
                val remainder = num % 100
                if (remainder == 0) {
                    farsiHundreds[hundreds]
                } else {
                    "${farsiHundreds[hundreds]} و ${convertNumberToPersianWords(remainder)}"
                }
            }
            else -> num.toString()
        }
    }

    private fun farsiToEnglishDigits(input: String): String {
        val farsiChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var output = input
        for (i in 0..9) {
            output = output.replace(farsiChars[i], i.toString()[0])
            output = output.replace(arabicChars[i], i.toString()[0])
        }
        return output
    }

    private fun convertStringWithNumbersToPersian(input: String): String {
        var normalized = farsiToEnglishDigits(input.trim())
        normalized = normalized.replace("-", " تا ")
        
        if (normalized.contains(" تا ")) {
            val parts = normalized.split(" تا ")
            if (parts.size == 2) {
                val p1 = parts[0].trim().toIntOrNull()
                val p2 = parts[1].trim().toIntOrNull()
                if (p1 != null && p2 != null) {
                    return "${convertNumberToPersianWords(p1)} تا ${convertNumberToPersianWords(p2)}"
                }
            }
        }
        val singleNum = normalized.toIntOrNull()
        if (singleNum != null) {
            return convertNumberToPersianWords(singleNum)
        }
        return input
    }

    fun release() {
        engineScope.cancel()
    }
}
