package com.moodarchive.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.moodarchive.domain.model.User
import com.moodarchive.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация AuthRepository через Firebase Authentication.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser()
                ?: return Result.failure(Exception("Не удалось получить данные пользователя"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser()
                ?: return Result.failure(Exception("Не удалось создать пользователя"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun isSignedIn(): Boolean = auth.currentUser != null

    override fun currentUid(): String? = auth.currentUser?.uid

    // ─── Маппер ───────────────────────────────────────────

    private fun com.google.firebase.auth.FirebaseUser.toUser() = User(
        uid         = uid,
        email       = email,
        displayName = displayName
    )
}
