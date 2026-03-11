package com.example.memoice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memoice.repository.MemoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MemoItem(
    val file: File,
    val durationStr: String
)

class HomeViewModel(private val repository: MemoRepository) : ViewModel() {

    private val _records = MutableStateFlow<List<MemoItem>>(emptyList())
    val records: StateFlow<List<MemoItem>> = _records.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                val files = repository.getRecords()
                
                // Trasformiamo ogni File in un MemoItem
                files.map { file ->
                    val durationSeconds = repository.getDurationSeconds(file)
                    
                    val minutes = durationSeconds / 60
                    val seconds = durationSeconds % 60
                    val formattedDuration = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    
                    MemoItem(file = file, durationStr = formattedDuration)
                }
            }
            // Aggiorniamo lo stato solo quando tutti i calcoli sono finiti
            _records.value = items
        }
    }

    fun deleteRecord(memo: MemoItem) {
        viewModelScope.launch {
            val success = repository.deleteRecord(memo.file)
            if (success) {
                loadRecords()
            }
        }
    }
}

class HomeViewModelFactory(private val repository: MemoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}