package com.moodarchive.domain.model

/**
 * Данные авторизованного пользователя.
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?
)
