package com.studio71.germanlinia2_b2.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.ExtraNoteEntity
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExtraNotesUiState(
    val notes: List<ExtraNoteEntity> = emptyList()
)

class ExtraNotesViewModel(
    private val repo: VocabularyRepository
) : ViewModel() {

    val state: StateFlow<ExtraNotesUiState> =
        repo.observeAllNotes()
            .map { notes -> ExtraNotesUiState(notes = notes) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExtraNotesUiState())

    fun saveNote(id: Int, title: String, content: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (id > 0) repo.getNoteById(id) else null
            val note = if (existing != null) {
                existing.copy(title = title, content = content, updatedAtEpochMs = now)
            } else {
                ExtraNoteEntity(title = title, content = content, createdAtEpochMs = now, updatedAtEpochMs = now)
            }
            repo.upsertNote(note)
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repo.deleteNote(id)
        }
    }

    class Factory(private val repo: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExtraNotesViewModel(repo) as T
    }
}

