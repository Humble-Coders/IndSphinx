package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import com.humblesolutions.indsphinx.repository.BackendComplaintRepository
import com.humblesolutions.indsphinx.repository.BackendComplaintTemplateRepository
import com.humblesolutions.indsphinx.repository.BackendStorageRepository
import com.humblesolutions.indsphinx.usecase.FetchComplaintTemplatesUseCase
import com.humblesolutions.indsphinx.usecase.SubmitComplaintUseCase
import kotlinx.coroutines.Dispatchers
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
}

class ComplaintsViewModel(application: Application) : AndroidViewModel(application) {
    private val fetchTemplatesUseCase = FetchComplaintTemplatesUseCase(BackendComplaintTemplateRepository())
    private val submitComplaintUseCase = SubmitComplaintUseCase(BackendComplaintRepository())
    private val storageRepo = BackendStorageRepository()

    private val _uiState = MutableStateFlow<ComplaintsUiState>(ComplaintsUiState.Landing)
    val uiState: StateFlow<ComplaintsUiState> = _uiState.asStateFlow()

    fun onAddComplaintClick() {
        viewModelScope.launch {
            _uiState.value = ComplaintsUiState.LoadingTemplates
            try {
                val templates = fetchTemplatesUseCase.execute()
                _uiState.value = ComplaintsUiState.SelectCategory(templates)
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

                // Upload all files in parallel
                val mediaUrls = coroutineScope {
                    mediaUris.mapIndexed { index, uri ->
                        async(Dispatchers.IO) {
                            val ext = BackendStorageRepository.extensionForUri(uri, context)
                            val uploadUri = if (ext == "jpg") {
                                compressImageUri(uri) ?: uri
                            } else {
                                uri
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

    fun dismissSuccess() {
        _uiState.value = ComplaintsUiState.Landing
    }

    fun dismissError() {
        _uiState.value = ComplaintsUiState.Landing
    }
}
