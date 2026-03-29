package com.humblesolutions.indsphinx.repository

import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.indsphinx.model.User
import kotlinx.coroutines.tasks.await

class AndroidAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    override suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Authentication failed")
        return User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty())
    }

    override suspend fun signUp(email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Account creation failed")
        return User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty())
    }

    override fun signOut() = auth.signOut()

    override fun getCurrentUser(): User? {
        return auth.currentUser?.let { User(uid = it.uid, email = it.email.orEmpty()) }
    }
}
