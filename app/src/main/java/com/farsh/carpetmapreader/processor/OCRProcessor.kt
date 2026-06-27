package com.farsh.carpetmapreader.processor

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OCRProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognize text in a crop of a Bitmap.
     * Uses suspendCancellableCoroutine to turn ML Kit Tasks into synchronous suspend calls.
     */
    suspend fun recognizeText(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text.trim()
                        .replace("\n", " ")
                        .filter { it.isDigit() || it.isLetter() } // Keep digits and clear noise
                    
                    if (detectedText.isNotEmpty()) {
                        continuation.resume(detectedText)
                    } else {
                        // Sometimes text-recognition reads letters or close shapes, we try to grab block text
                        val fallbackText = visionText.textBlocks.firstOrNull()?.text?.trim()
                            ?.filter { it.isDigit() }
                        continuation.resume(if (!fallbackText.isNullOrEmpty()) fallbackText else null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("OCRProcessor", "ML Kit recognition failure: ", e)
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            Log.e("OCRProcessor", "Exception running OCR", e)
            continuation.resume(null)
        }
    }
}
