package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.repository.BackendOccupantAssetRepository
import com.humblesolutions.indsphinx.usecase.ObserveOccupantAssetsUseCase
import com.humblesolutions.indsphinx.usecase.OccupantAssetsSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AssetsUiState {
    object Loading : AssetsUiState()
    data class Loaded(val snapshot: OccupantAssetsSnapshot) : AssetsUiState()
    data class Error(val message: String) : AssetsUiState()
}

class AssetsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = BackendOccupantAssetRepository()
    private val observeUseCase = ObserveOccupantAssetsUseCase(repo)

    private val _uiState = MutableStateFlow<AssetsUiState>(AssetsUiState.Loading)
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    private var listenerJob: Job? = null
    private var listeningUid: String = ""

    /**
     * Deep-link entry can call this before the profile has loaded, so blank
     * uids are ignored: the screen re-fires once the real uid arrives. Calling
     * again with the same uid keeps the existing listener.
     */
    fun start(authUid: String) {
        if (authUid.isBlank()) return
        if (listenerJob?.isActive == true && listeningUid == authUid) return
        listenerJob?.cancel()
        listeningUid = authUid
        _uiState.value = AssetsUiState.Loading
        listenerJob = viewModelScope.launch {
            try {
                observeUseCase.execute(authUid).collect { snapshot ->
                    _uiState.value = AssetsUiState.Loaded(snapshot)
                }
            } catch (e: Exception) {
                _uiState.value = AssetsUiState.Error(e.message ?: "Could not load your assets.")
            }
        }
    }

    override fun onCleared() {
        listenerJob?.cancel()
        super.onCleared()
    }
}
