package com.moodarchive.domain.repository

import com.moodarchive.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория авторизации (Firebase Auth).
 */
interface AuthRepository {

    /** Текущий авторизованный пользователь (null = не вошёл) */
    val currentUser: Flow<User?>

    /** Войти по email + пароль */
    suspend fun signIn(email: String, password: String): Result<User>

    /** Зарегистрироваться по email + пароль */
    suspend fun signUp(email: String, password: String): Result<User>

    /** Выйти из аккаунта */
    suspend fun signOut()

    /** Проверить, авторизован ли пользователь сейчас */
    fun isSignedIn(): Boolean

    /** UID текущего пользователя или null */
    fun currentUid(): String?
}
