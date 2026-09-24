package com.sidequests.app.data.photo

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoProofUpload(
    val storagePath: String,
    val byteCount: Long,
)

interface PhotoProofRepository {
    suspend fun upload(
        attemptId: String,
        questId: String,
        stepIndex: Int,
        photoFile: File,
    ): Result<PhotoProofUpload>
}

class SupabasePhotoProofRepository(
    private val client: SupabaseClient,
    private val bucketName: String,
) : PhotoProofRepository {
    override suspend fun upload(
        attemptId: String,
        questId: String,
        stepIndex: Int,
        photoFile: File,
    ): Result<PhotoProofUpload> = runCatching {
        withContext(Dispatchers.IO) {
            check(photoFile.isFile && photoFile.length() > 0) {
                "The captured photo is empty or unavailable."
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: error("No authenticated Supabase user is available.")
            val safeQuestId = safePathSegment(questId)
            val storagePath = listOf(
                userId,
                attemptId,
                safeQuestId,
                "step-${stepIndex + 1}-${UUID.randomUUID()}.jpg",
            ).joinToString("/")

            client.storage.from(bucketName).upload(storagePath, photoFile.readBytes()) {
                upsert = false
                contentType = ContentType.Image.JPEG
            }

            PhotoProofUpload(
                storagePath = storagePath,
                byteCount = photoFile.length(),
            )
        }
    }

    private companion object {
        val UNSAFE_PATH_CHARACTER = Regex("[^A-Za-z0-9._-]")

        fun safePathSegment(value: String): String = value
            .replace(UNSAFE_PATH_CHARACTER, "_")
            .trim('.', '_')
            .ifBlank { "quest" }
    }
}
