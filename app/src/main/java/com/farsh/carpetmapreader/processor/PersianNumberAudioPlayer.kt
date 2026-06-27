package com.farsh.carpetmapreader.processor

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.farsh.carpetmapreader.R

class PersianNumberRawAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<Int> = emptyList()
    private var playlistIndex = 0
    private var onDoneCallback: (() -> Unit)? = null

    companion object {
        private const val TAG = "PersianAudio"
    }

    fun getRawResIdsForNumber(number: Int): List<Int> {
        if (number <= 0) return emptyList()

        val result = mutableListOf<Int>()
        var temp = number

        if (temp >= 100) {
            val hundreds = (temp / 100) * 100
            val rem = temp % 100
            val resId = getResIdOrNull("n${hundreds}" + if (rem > 0) "_join" else "")
            if (resId != null) result.add(resId)
            temp = rem
        }

        if (temp >= 20) {
            val tens = (temp / 10) * 10
            val rem = temp % 10
            if (rem > 0) {
                val tensId = getResIdOrNull("n${tens}_join")
                if (tensId != null) result.add(tensId)
                val unitId = getResIdOrNull("n$rem")
                if (unitId != null) result.add(unitId)
            } else {
                val tensId = getResIdOrNull("n$tens")
                if (tensId != null) result.add(tensId)
            }
        } else if (temp >= 10) {
            val id = getResIdOrNull("n$temp")
            if (id != null) result.add(id)
        } else if (temp > 0) {
            val id = getResIdOrNull("n$temp")
            if (id != null) result.add(id)
        }

        return result
    }

    private fun getResIdOrNull(name: String): Int? {
        return try {
            val field = R.raw::class.java.getField(name)
            val id = field.getInt(null)
            if (id != 0) id else null
        } catch (e: Exception) {
            Log.e(TAG, "Resource not found: R.raw.$name")
            null
        }
    }

    fun playNumber(number: Int, onDone: () -> Unit) {
        stop()
        val resIds = getRawResIdsForNumber(number)
        Log.d(TAG, "playNumber called for: $number -> $resIds")

        if (resIds.isEmpty()) {
            Log.e(TAG, "No audio resources found for number: $number")
            onDone()
            return
        }

        this.playlist = resIds
        this.playlistIndex = 0
        this.onDoneCallback = onDone

        playNext()
    }

    private fun playNext() {
        if (playlistIndex >= playlist.size) {
            Log.d(TAG, "Finished playback of all files.")
            onDoneCallback?.invoke()
            return
        }

        val resId = playlist[playlistIndex]
        playlistIndex++

        Log.d(TAG, "Playing resId=$resId")

        try {
            val player = MediaPlayer.create(context, resId)
            if (player == null) {
                Log.e(TAG, "MediaPlayer.create returned null for resId=$resId")
                playNext()
                return
            }

            mediaPlayer = player.apply {
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                    playNext()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error for resId=$resId: ($what, $extra)")
                    mp.release()
                    if (mediaPlayer == mp) mediaPlayer = null
                    playNext()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception playing resId=$resId. Skipping.", e)
            playNext()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        }
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
    }
}
