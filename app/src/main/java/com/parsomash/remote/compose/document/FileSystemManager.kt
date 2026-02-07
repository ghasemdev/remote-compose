package com.parsomash.remote.compose.document

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Interface for managing document file storage
 */
interface FileSystemManager {
    /**
     * Saves a document to storage
     * 
     * @param documentId Unique identifier for the document
     * @param bytes Document bytes to save
     */
    suspend fun saveDocument(documentId: String, bytes: ByteArray)
    
    /**
     * Loads a document from storage
     * 
     * @param documentId Unique identifier for the document
     * @return Document bytes if found, null otherwise
     */
    suspend fun loadDocument(documentId: String): ByteArray?
    
    /**
     * Deletes a document from storage
     * 
     * @param documentId Unique identifier for the document
     */
    suspend fun deleteDocument(documentId: String)
    
    /**
     * Lists all available document IDs
     * 
     * @return List of document IDs
     */
    suspend fun listDocuments(): List<String>
    
    /**
     * Gets the documents directory path
     * 
     * @return Path to the documents directory
     */
    fun getDocumentsDirectory(): String
}

/**
 * Implementation of FileSystemManager for Android
 * 
 * Manages local file storage for generated Remote Compose documents
 */
class FileSystemManagerImpl(
    private val context: Context
) : FileSystemManager {
    
    private val documentsDir: File by lazy {
        File(context.filesDir, "remote_compose_documents").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    override suspend fun saveDocument(documentId: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, "$documentId.rcd") // Remote Compose Document
            file.writeBytes(bytes)
        } catch (e: IOException) {
            throw DocumentStorageException("Failed to save document $documentId", e)
        }
    }
    
    override suspend fun loadDocument(documentId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, "$documentId.rcd")
            if (file.exists()) {
                file.readBytes()
            } else {
                null
            }
        } catch (e: IOException) {
            throw DocumentStorageException("Failed to load document $documentId", e)
        }
    }
    
    override suspend fun deleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, "$documentId.rcd")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: IOException) {
            throw DocumentStorageException("Failed to delete document $documentId", e)
        }
    }
    
    override suspend fun listDocuments(): List<String> = withContext(Dispatchers.IO) {
        try {
            documentsDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".rcd") }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: IOException) {
            throw DocumentStorageException("Failed to list documents", e)
        }
    }
    
    override fun getDocumentsDirectory(): String = documentsDir.absolutePath
}

/**
 * Exception thrown when document storage operations fail
 */
class DocumentStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)