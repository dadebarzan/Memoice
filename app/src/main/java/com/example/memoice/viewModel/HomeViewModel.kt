package com.example.memoice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memoice.repository.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(private val repository: MemoRepository) : ViewModel() {

    // Usiamo uno StateFlow per far "osservare" la lista alla UI (Jetpack Compose)
    private val _records = MutableStateFlow<List<File>>(emptyList())
    val records: StateFlow<List<File>> = _records.asStateFlow()

    init {
        // Appena il ViewModel viene creato, carichiamo i file
        loadRecords()
    }

    // Chiede al repository di leggere i file e aggiorna lo stato
    fun loadRecords() {
        viewModelScope.launch {
            _records.value = repository.getRecords()
        }
    }

    // Chiede al repository di eliminare il file e, se va a buon fine, ricarica la lista
    fun deleteRecord(file: File) {
        viewModelScope.launch {
            val success = repository.deleteRecord(file)
            if (success) {
                loadRecords()
            }
        }
    }
}

// Questa Factory ci serve perché il nostro ViewModel ha bisogno del Repository come parametro
class HomeViewModelFactory(private val repository: MemoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}