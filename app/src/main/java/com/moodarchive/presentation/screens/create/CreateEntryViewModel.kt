package com.moodarchive.presentation.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.model.Attachment
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion
import com.moodarchive.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class CreateEntryUiState(
    val text: String = "",
    val selectedEmotion: Emotion = Emotion.NEUTRAL,
    val attachments: List<Attachment> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val editingEntryId: String? = null,
    val error: String? = null
)

@HiltViewModel
class CreateEntryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEntryUiState())
    val uiState: StateFlow<CreateEntryUiState> = _uiState.asStateFlow()

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            if (entry != null) {
                _uiState.value = _uiState.value.copy(
                    text = entry.text,
                    selectedEmotion = entry.emotion,
                    attachments = entry.attachments,
                    isEditing = true,
                    editingEntryId = entryId
                )
            }
        }
    }

    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(text = text)
    }

    fun selectEmotion(emotion: Emotion) {
        _uiState.value = _uiState.value.copy(selectedEmotion = emotion)
    }

    fun addAttachmentFromUri(uri: Uri, type: com.moodarchive.domain.model.AttachmentType, fallbackName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: ""
                var actualFileName = fallbackName
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        actualFileName = cursor.getString(nameIndex)
                    }
                }

                val destFile = File(context.filesDir, "attach_${UUID.randomUUID()}_$actualFileName")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val internalUri = Uri.fromFile(destFile).toString()
                
                val attachment = Attachment(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    localUri = internalUri,
                    mimeType = mimeType,
                    fileName = actualFileName
                )
                
                _uiState.value = _uiState.value.copy(
                    attachments = _uiState.value.attachments + attachment
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка загрузки файла")
            }
        }
    }

    fun removeAttachment(attachment: Attachment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(Uri.parse(attachment.localUri).path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore delete errors
            }
            _uiState.value = _uiState.value.copy(
                attachments = _uiState.value.attachments - attachment
            )
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.text.isBlank()) {
            _uiState.value = state.copy(error = "Текст записи не может быть пустым")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                if (state.isEditing && state.editingEntryId != null) {
                    val entry = DiaryEntry(
                        id = state.editingEntryId,
                        text = state.text,
                        emotion = state.selectedEmotion,
                        attachments = state.attachments
                    )
                    repository.updateEntry(entry)
                } else {
                    val entry = DiaryEntry(
                        text = state.text,
                        emotion = state.selectedEmotion,
                        attachments = state.attachments
                    )
                    repository.insertEntry(entry)
                }
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Ошибка сохранения"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteCurrentEntry(onDeleted: () -> Unit) {
        val id = _uiState.value.editingEntryId ?: return
        viewModelScope.launch {
            try {
                repository.deleteEntry(id)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка удаления")
            }
        }
    }

    fun resetSaved() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }
}
