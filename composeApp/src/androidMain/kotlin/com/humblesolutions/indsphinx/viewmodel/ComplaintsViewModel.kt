package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import com.humblesolutions.indsphinx.repository.BackendComplaintRepository
import com.humblesolutions.indsphinx.repository.BackendComplaintTemplateRepository
import com.humblesolutions.indsphinx.repository.BackendStorageRepository
import com.humblesolutions.indsphinx.usecase.CloseComplaintUseCase
import com.humblesolutions.indsphinx.usecase.SubmitComplaintUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class ComplaintsUiState {
    object Landing : ComplaintsUiState()
    object LoadingTemplates : ComplaintsUiState()
    data class SelectCategory(val templates: List<ComplaintTemplate>) : ComplaintsUiState()
    data class SubmitForm(
        val templates: List<ComplaintTemplate>,
        val selectedTemplate: ComplaintTemplate
    ) : ComplaintsUiState()
    object Submitting : ComplaintsUiState()
    object Success : ComplaintsUiState()
    data class Error(val message: String) : ComplaintsUiState()
    object LoadingComplaints : ComplaintsUiState()
    data class ViewComplaints(val complaints: List<Complaint>) : ComplaintsUiState()
    data class ComplaintDetail(val complaint: Complaint, val complaints: List<Complaint>) : ComplaintsUiState()
}

class ComplaintsViewModel(application: Application) : AndroidViewModel(application) {
    private val templateRepo = BackendComplaintTemplateRepository()
    private val complaintRepo = BackendComplaintRepository()
    private val submitComplaintUseCase = SubmitComplaintUseCase(complaintRepo)
    private val closeComplaintUseCase = CloseComplaintUseCase(complaintRepo)
    private val storageRepo = BackendStorageRepository()

    private val _uiState = MutableStateFlow<ComplaintsUiState>(ComplaintsUiState.Landing)
    val uiState: StateFlow<ComplaintsUiState> = _uiState.asStateFlow()

    private var templatesJob: Job? = null
    private var complaintsJob: Job? = null

    fun onAddComplaintClick() {
        _uiState.value = ComplaintsUiState.LoadingTemplates
        templatesJob?.cancel()
        templatesJob = viewModelScope.launch {
            try {
                templateRepo.observeTemplates().collect { templates ->
                    when (_uiState.value) {
                        is ComplaintsUiState.LoadingTemplates,
                        is ComplaintsUiState.SelectCategory -> _uiState.value = ComplaintsUiState.SelectCategory(templates)
                        is ComplaintsUiState.SubmitForm -> {
                            val cur = _uiState.value as ComplaintsUiState.SubmitForm
                            _uiState.value = cur.copy(templates = templates)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ComplaintsUiState.Error(e.message ?: "Failed to load categories")
            }
        }
    }

    fun onCategorySelected(template: ComplaintTemplate) {
        val templates = (_uiState.value as? ComplaintsUiState.SelectCategory)?.templates ?: emptyList()
        _uiState.value = ComplaintsUiState.SubmitForm(templates, template)
    }

    fun onBackFromCategory() {
        templatesJob?.cancel()
        templatesJob = null
        _uiState.value = ComplaintsUiState.Landing
    }

    fun onBackFromForm() {
        val templates = (_uiState.value as? ComplaintsUiState.SubmitForm)?.templates ?: emptyList()
        _uiState.value = ComplaintsUiState.SelectCategory(templates)
    }

    fun submitComplaint(
        problem: String,
        description: String,
        priority: String,
        occupantName: String,
        occupantEmail: String,
        occupantId: String,
        flatNumber: String,
        flatId: String,
        mediaUris: List<Uri> = emptyList()
    ) {
        val template = (_uiState.value as? ComplaintsUiState.SubmitForm)?.selectedTemplate ?: return
        viewModelScope.launch {
            _uiState.value = ComplaintsUiState.Submitting
            try {
                val context = getApplication<Application>()
                val uploadId = BackendStorageRepository.generateUploadId()

                // Upload all files in parallel (with compression)
                val mediaUrls = coroutineScope {
                    mediaUris.mapIndexed { index, uri ->
                        async(Dispatchers.IO) {
                            val ext = BackendStorageRepository.extensionForUri(uri, context)
                            val uploadUri = when (ext) {
                                "jpg" -> compressImageUri(uri) ?: uri
                                "mp4" -> compressVideoUri(uri, index)
                                else -> uri
                            }
                            storageRepo.uploadFile(uploadUri, context, "complaints/$uploadId/$index.$ext")
                        }
                    }.awaitAll()
                }

                val complaint = Complaint(
                    flatNumber = flatNumber,
                    flatId = flatId,
                    occupantEmail = occupantEmail,
                    occupantName = occupantName,
                    occupantId = occupantId,
                    category = template.category,
                    date = System.currentTimeMillis(),
                    status = "OPEN",
                    priority = priority,
                    description = description,
                    problem = problem,
                    mediaUrls = mediaUrls
                )
                submitComplaintUseCase.execute(complaint)
                _uiState.value = ComplaintsUiState.Success
            } catch (e: Exception) {
                _uiState.value = ComplaintsUiState.Error(e.message ?: "Failed to submit complaint")
            }
        }
    }

    /**
     * Scales image to max 1280px on longest side and compresses to 75% JPEG quality.
     * Reduces a typical 5-8 MB phone photo to ~250-400 KB (15-20x smaller).
     */
    private fun compressImageUri(uri: Uri): Uri? {
        return try {
            val context = getApplication<Application>()
            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val maxDim = 1280
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                ).also { original.recycle() }
            } else {
                original
            }

            val file = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            scaled.recycle()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null // fall back to original URI on any failure
        }
    }

    /**
     * Compresses video using LightCompressor (MEDIUM quality, native MediaCodec).
     * Typical 50-80 MB video → 5-15 MB (5-10× smaller), much faster upload.
     * Falls back to original URI on any failure.
     */
    private suspend fun compressVideoUri(uri: Uri, index: Int): Uri {
        val context = getApplication<Application>()
        val originalSize = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0L

        // Skip compression for videos under 50MB — not worth the time
        if (originalSize < 80 * 1024 * 1024) {
            Log.d("VideoCompressor", "[$index] Skipping compression (${originalSize / 1024} KB < 50MB), uploading original")
            return uri
        }

        Log.d("VideoCompressor", "[$index] Compressing large video (${originalSize / 1024 / 1024} MB)")
        val deferred = CompletableDeferred<Uri>()
        val startTime = System.currentTimeMillis()

        VideoCompressor.start(
            context = context,
            uris = listOf(uri),
            isStreamable = true,
            storageConfiguration = AppSpecificStorageConfiguration(subFolderName = "compressed_videos"),
            configureWith = Configuration(
                quality = VideoQuality.MEDIUM,
                isMinBitrateCheckEnabled = false,
                keepOriginalResolution = false,
                videoNames = listOf("compressed_$index.mp4")
            ),
            listener = object : CompressionListener {
                override fun onSuccess(index: Int, size: Long, path: String?) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (path != null) {
                        val finalPath = if (path.endsWith("_temp")) path.removeSuffix("_temp") else path
                        val file = File(finalPath)
                        if (file.exists()) {
                            Log.d("VideoCompressor", "[$index] Compressed: ${file.length() / 1024} KB | Time: ${elapsed}ms")
                            deferred.complete(Uri.fromFile(file))
                        } else {
                            Log.w("VideoCompressor", "[$index] File not found, falling back")
                            deferred.complete(uri)
                        }
                    } else {
                        deferred.complete(uri)
                    }
                }
                override fun onFailure(index: Int, failureMessage: String) { deferred.complete(uri) }
                override fun onCancelled(index: Int) { deferred.complete(uri) }
                override fun onProgress(index: Int, percent: Float) {}
                override fun onStart(index: Int) {}
            }
        )

        return deferred.await()
    }
    fun dismissSuccess() {
        _uiState.value = ComplaintsUiState.Landing
    }

    fun dismissError() {
        _uiState.value = ComplaintsUiState.Landing
    }

    fun onViewComplaintsClick(occupantId: String) {
        _uiState.value = ComplaintsUiState.LoadingComplaints
        complaintsJob?.cancel()
        complaintsJob = viewModelScope.launch {
            try {
                complaintRepo.observeByOccupant(occupantId).collect { complaints ->
                    when (val cur = _uiState.value) {
                        is ComplaintsUiState.LoadingComplaints,
                        is ComplaintsUiState.ViewComplaints -> _uiState.value = ComplaintsUiState.ViewComplaints(complaints)
                        is ComplaintsUiState.ComplaintDetail -> {
                            val refreshed = complaints.find { it.id == cur.complaint.id } ?: cur.complaint
                            _uiState.value = ComplaintsUiState.ComplaintDetail(refreshed, complaints)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ComplaintsUiState.Error(e.message ?: "Failed to load complaints")
            }
        }
    }

    fun onComplaintSelected(complaint: Complaint) {
        val complaints = (_uiState.value as? ComplaintsUiState.ViewComplaints)?.complaints ?: emptyList()
        _uiState.value = ComplaintsUiState.ComplaintDetail(complaint, complaints)
    }

    fun onBackFromDetail() {
        val complaints = (_uiState.value as? ComplaintsUiState.ComplaintDetail)?.complaints ?: emptyList()
        _uiState.value = ComplaintsUiState.ViewComplaints(complaints)
    }

    fun onBackFromComplaints() {
        complaintsJob?.cancel()
        complaintsJob = null
        _uiState.value = ComplaintsUiState.Landing
    }

    fun closeComplaint(complaintId: String, occupantId: String) {
        viewModelScope.launch {
            try {
                closeComplaintUseCase.execute(complaintId)
                // Listener auto-updates the list; just navigate back to list view
                val complaints = (_uiState.value as? ComplaintsUiState.ComplaintDetail)?.complaints ?: emptyList()
                _uiState.value = ComplaintsUiState.ViewComplaints(complaints)
            } catch (e: Exception) {
                _uiState.value = ComplaintsUiState.Error(e.message ?: "Failed to close complaint")
            }
        }
    }
}
