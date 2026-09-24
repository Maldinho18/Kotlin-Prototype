package com.sidequests.app.sensor.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class PendingPhotoCapture(
    val uri: Uri,
    val file: File,
    val stepIndex: Int,
)

/**
 * Owns the Android-specific file and content-URI details for a full-resolution camera capture.
 * The UI only launches the camera and returns the resulting file to the ViewModel.
 */
class CameraPhotoProofService(
    private val context: Context,
) {
    fun createCapture(questId: String, stepIndex: Int): PendingPhotoCapture {
        val safeQuestId = questId.replace(UNSAFE_PATH_CHARACTER, "_")
        val proofDirectory = File(context.filesDir, "quest-proofs/$safeQuestId")
        check(proofDirectory.mkdirs() || proofDirectory.isDirectory) {
            "Could not create the local photo-proof directory."
        }

        val file = File.createTempFile(
            "step-${stepIndex + 1}-",
            ".jpg",
            proofDirectory,
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        return PendingPhotoCapture(uri = uri, file = file, stepIndex = stepIndex)
    }

    fun discardCapture(file: File) {
        if (file.exists()) file.delete()
    }

    private companion object {
        val UNSAFE_PATH_CHARACTER = Regex("[^A-Za-z0-9._-]")
    }
}
