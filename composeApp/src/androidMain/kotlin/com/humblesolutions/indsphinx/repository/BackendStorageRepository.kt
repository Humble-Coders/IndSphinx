package com.humblesolutions.indsphinx.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BackendStorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadFile(uri: Uri, context: Context, uploadPath: String): String {
        val ref = storage.reference.child(uploadPath)
        if (uri.scheme == "file") {
            context.contentResolver.openInputStream(uri)!!.use { stream ->
                ref.putStream(stream).await()
            }
        } else {
            ref.putFile(uri).await()
        }
        return ref.downloadUrl.await().toString()
    }

    companion object {
        fun generateUploadId(): String = UUID.randomUUID().toString()

        fun extensionForUri(uri: Uri, context: Context): String {
            val mime = context.contentResolver.getType(uri) ?: return "jpg"
            return if (mime.startsWith("video/")) "mp4" else "jpg"
        }
    }
}
