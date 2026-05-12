package com.moodarchive.domain.model

/**
 * Перечисление эмоций, доступных для выбора пользователем.
 * Каждая эмоция имеет отображаемое имя, иконку (emoji) и цвет для UI.
 */
enum class Emotion(
    val displayName: String,
    val emoji: String,
    val colorHex: Long
) {
    HAPPY("Радость", "😊", 0xFFFFC107),
    SAD("Грусть", "😢", 0xFF5C6BC0),
    ANXIOUS("Тревога", "😰", 0xFFFF7043),
    ANGRY("Злость", "😠", 0xFFE53935),
    CALM("Спокойствие", "😌", 0xFF66BB6A),
    EXCITED("Восторг", "🤩", 0xFFAB47BC),
    TIRED("Усталость", "😴", 0xFF78909C),
    GRATEFUL("Благодарность", "🙏", 0xFF26A69A),
    INSPIRED("Вдохновение", "✨", 0xFFFFCA28),
    NEUTRAL("Нейтрально", "😐", 0xFF90A4AE);

    companion object {
        fun fromName(name: String): Emotion {
            return entries.find { it.name == name } ?: NEUTRAL
        }
    }
}
