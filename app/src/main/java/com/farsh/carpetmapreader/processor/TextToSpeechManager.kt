package com.farsh.carpetmapreader.processor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class TextToSpeechManager(
    private val context: Context,
    private val onInitSuccess: () -> Unit = {}
) {
    var isInitialized = true
        private set
    var isPersianSupported = true
        private set

    private val persianPlayer = PersianNumberRawAudioPlayer(context)

    val hasCustomAudioAssets: Boolean
        get() = true

    private var onSpeechDoneCallback: ((String) -> Unit)? = null
    private var currentUtteranceId: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        Log.d("TextToSpeechManager", "Initializing Raw Audio-based Persian Number Player")
        mainHandler.post {
            onInitSuccess()
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

    private fun extractNumbers(text: String): List<Int> {
        val normalized = farsiToEnglishDigits(text)
        val regex = Regex("\\d+")
        return regex.findAll(normalized)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
    }

    fun speak(text: String, utteranceId: String, speed: Float = 1.0f): Boolean {
        stop()
        currentUtteranceId = utteranceId

        val numbers = extractNumbers(text)
        Log.d("TextToSpeechManager", "speak text='$text' -> extracted numbers: $numbers")

        if (numbers.isEmpty()) {
            mainHandler.post {
                triggerOnDone()
            }
            return true
        }

        playNumbersSequentially(numbers) {
            triggerOnDone()
        }
        return true
    }

    private fun playNumbersSequentially(numbers: List<Int>, callback: () -> Unit) {
        var index = 0
        fun playNext() {
            if (index >= numbers.size) {
                callback()
                return
            }
            val num = numbers[index]
            index++
            persianPlayer.playNumber(num) {
                playNext()
            }
        }
        playNext()
    }

    private fun triggerOnDone() {
        Log.d("TextToSpeechManager", "Finished sequence for speech utterance id: $currentUtteranceId")
        onSpeechDoneCallback?.invoke(currentUtteranceId)
    }

    fun stop() {
        persianPlayer.stop()
    }

    fun setSpeechDoneListener(listener: (String) -> Unit) {
        onSpeechDoneCallback = listener
    }

    fun shutdown() {
        persianPlayer.shutdown()
    }
}
