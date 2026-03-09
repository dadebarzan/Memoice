package com.example.memoice.repository

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MemoRepository(val folder: File) {

    // Recupera la lista dei file audio in modo asincrono (fuori dal Main Thread)
    suspend fun getRecords(): List<File> = withContext(Dispatchers.IO) {
        folder.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // Elimina un file audio
    suspend fun deleteRecord(file: File): Boolean = withContext(Dispatchers.IO) {
        file.delete()
    }

    // Rinomina un file audio in modo sicuro
    suspend fun renameRecord(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val newFile = File(folder, "$newName.${file.extension}")
        file.renameTo(newFile)
    }

    // Ottiene la durata reale dell'audio leggendo i metadati (metodo ortodosso)
    fun getDurationSeconds(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
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