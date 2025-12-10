package com.example.glytch

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Runs ML Kit OCR on a bitmap and returns the full recognized text.
 */
fun runOcrOnBitmap(
    bitmap: Bitmap,
    onResult: (String) -> Unit
) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            onResult(visionText.text)
        }
        .addOnFailureListener { e ->
            onResult("OCR failed: ${e.message}")
        }
}
