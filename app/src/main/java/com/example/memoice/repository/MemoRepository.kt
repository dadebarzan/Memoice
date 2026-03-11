package com.example.memoice.repository

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MemoRepository(val folder: File) {

    // Recupera la lista dei file audio in modo asincrono
    suspend fun getRecords(): List<File> = withContext(Dispatchers.IO) {
        folder.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // Elimina un file audio
    suspend fun deleteRecord(file: File): Boolean = withContext(Dispatchers.IO) {
        file.delete()
    }

    // Rinomina un file audio in modo sicuro
    suspend fun renameRecord(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val sanitizedName = newName.trim()

        // Nome non valido: vuoto o con separatori di percorso
        if (sanitizedName.isEmpty() ||
            sanitizedName.contains(File.separatorChar) ||
            sanitizedName.contains('/') ||
            sanitizedName.contains('\\')
        ) {
            return@withContext false
        }

        val newFile = File(folder, "$sanitizedName.${file.extension}")

        // Evita di sovrascrivere un file diverso con lo stesso nome
        if (newFile.exists() && newFile.absolutePath != file.absolutePath) {
            return@withContext false
        }

        try {
            file.renameTo(newFile)
        } catch (e: SecurityException) {
            false
        }
    }

    // Ottiene la durata reale dell'audio leggendo i metadati
    suspend fun getDurationSeconds(file: File): Int = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLong() ?: 0L
            (timeInMillis / 1000).toInt()
        } catch (e: Exception) {
            0
        } finally {
            retriever.release()
        }
    }
}