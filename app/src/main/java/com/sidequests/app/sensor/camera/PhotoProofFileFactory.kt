package com.sidequests.app.sensor.camera

import java.io.File

/** Product consumed by the camera service, independent of Android URI handling. */
interface PhotoProofFile {
    val file: File
    val stepIndex: Int
}

/**
 * Factory Method creator: prepares a safe per-quest directory and delegates the
 * concrete evidence-file creation to [createProof].
 */
abstract class PhotoProofFileFactory(private val filesDirectory: File) {
    fun create(questId: String, stepIndex: Int): PhotoProofFile {
        require(stepIndex >= 0) { "A photo proof needs a valid quest step." }
        val safeQuestId = questId
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', '_')
            .ifBlank { "quest" }
        val directory = File(filesDirectory, "quest-proofs/$safeQuestId")
        check(directory.mkdirs() || directory.isDirectory) {
            "Could not create the local photo-proof directory."
        }
        return createProof(directory, stepIndex)
    }

    protected abstract fun createProof(directory: File, stepIndex: Int): PhotoProofFile
}

/** Concrete creator for the JPEG targets used by TakePicture and Supabase Storage. */
class JpegPhotoProofFileFactory(filesDirectory: File) : PhotoProofFileFactory(filesDirectory) {
    override fun createProof(directory: File, stepIndex: Int): PhotoProofFile =
        JpegPhotoProofFile(
            file = File.createTempFile("step-${stepIndex + 1}-", ".jpg", directory),
            stepIndex = stepIndex,
        )
}

private data class JpegPhotoProofFile(
    override val file: File,
    override val stepIndex: Int,
) : PhotoProofFile
