package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AndroidAuthRepository()

    val currentUser = authRepository.getCurrentUser()

    fun signOut() = authRepository.signOut()
}
