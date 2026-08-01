package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.repository.BackendOccupantAssetRepository
import com.humblesolutions.indsphinx.usecase.ObserveOccupantAssetsUseCase
import com.humblesolutions.indsphinx.usecase.OccupantAssetsSnapshot
import kotlinx.coroutines.CancellationException
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
            } catch (e: CancellationException) {
                // Screen closed or the uid changed: not a failure.
                throw e
            } catch (e: Exception) {
                Log.e("AssetsViewModel", "assets observe failed", e)
                _uiState.value = AssetsUiState.Error("Could not load your assets. Please try again.")
            }
        }
    }

    /**
     * Drops the Firestore listener when the screen leaves composition.
     *
     * This ViewModel is Activity-scoped, so without this the listener would
     * outlive the closed screen and run alongside HomeViewModel's listener on
     * the identical query for the rest of the session. Matches iOS, where the
     * @StateObject is torn down when the cover is dismissed.
     */
    fun stop() {
        listenerJob?.cancel()
        listenerJob = null
        listeningUid = ""
        _uiState.value = AssetsUiState.Loading
    }

    override fun onCleared() {
        listenerJob?.cancel()
        super.onCleared()
    }
}
