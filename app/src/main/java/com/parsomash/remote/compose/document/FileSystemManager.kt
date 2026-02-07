package com.parsomash.remote.compose.document

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Document metadata for file organization and management
 */
@Serializable
data class DocumentMetadata(
    val id: String,
    val version: String = "1.0.0",
    val title: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val filePath: String,
    val fileSize: Long = 0L,
    val checksum: String = ""
)

/**
 * File information for document storage
 */
@Serializable
data class FileInfo(
    val filePath: String,
    val fileSize: Long,
    val createdAt: Long,
    val checksum: String
)

/**
 * Interface for managing document file storage with metadata support
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
     * Saves a document with metadata to storage
     *
     * @param documentId Unique identifier for the document
     * @param bytes Document bytes to save
     * @param metadata Document metadata
     */
    suspend fun saveDocumentWithMetadata(
        documentId: String,
        bytes: ByteArray,
        metadata: DocumentMetadata
    )

    /**
     * Loads a document from storage
     *
     * @param documentId Unique identifier for the document
     * @return Document bytes if found, null otherwise
     */
    suspend fun loadDocument(documentId: String): ByteArray?

    /**
     * Loads document metadata from storage
     *
     * @param documentId Unique identifier for the document
     * @return Document metadata if found, null otherwise
     */
    suspend fun loadDocumentMetadata(documentId: String): DocumentMetadata?

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
     * Lists all documents with their metadata
     *
     * @return List of document metadata
     */
    suspend fun listDocumentsWithMetadata(): List<DocumentMetadata>

    /**
     * Gets the documents directory path
     *
     * @return Path to the documents directory
     */
    fun getDocumentsDirectory(): String

    /**
     * Gets document file size
     *
     * @param documentId Unique identifier for the document
     * @return File size in bytes, or null if document doesn't exist
     */
    suspend fun getDocumentSize(documentId: String): Long?

    /**
     * Checks if a document exists
     *
     * @param documentId Unique identifier for the document
     * @return true if document exists, false otherwise
     */
    suspend fun documentExists(documentId: String): Boolean
}

/**
 * Implementation of FileSystemManager for Android with metadata support
 *
 * Manages local file storage for generated Remote Compose documents with metadata
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

    private val metadataDir: File by lazy {
        File(documentsDir, "metadata").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveDocument(documentId: String, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            try {
                val file = File(documentsDir, "$documentId.rcd") // Remote Compose Document
                file.writeBytes(bytes)

                // Create basic metadata
                val currentTime = System.currentTimeMillis()
                val checksum = calculateChecksum(bytes)
                val metadata = DocumentMetadata(
                    id = documentId,
                    title = documentId,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    filePath = file.absolutePath,
                    fileSize = bytes.size.toLong(),
                    checksum = checksum
                )
                saveMetadata(documentId, metadata)
            } catch (e: IOException) {
                throw DocumentStorageException("Failed to save document $documentId", e)
            }
        }

    override suspend fun saveDocumentWithMetadata(
        documentId: String,
        bytes: ByteArray,
        metadata: DocumentMetadata
    ) = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, "$documentId.rcd")
            file.writeBytes(bytes)

            // Update metadata with actual file info
            val checksum = calculateChecksum(bytes)
            val updatedMetadata = metadata.copy(
                filePath = file.absolutePath,
                fileSize = bytes.size.toLong(),
                checksum = checksum,
                updatedAt = System.currentTimeMillis()
            )
            saveMetadata(documentId, updatedMetadata)
        } catch (e: IOException) {
            throw DocumentStorageException("Failed to save document $documentId with metadata", e)
        }
    }

    override suspend fun loadDocument(documentId: String): ByteArray? =
        withContext(Dispatchers.IO) {
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

    override suspend fun loadDocumentMetadata(documentId: String): DocumentMetadata? =
        withContext(Dispatchers.IO) {
            try {
                val metadataFile = File(metadataDir, "$documentId.json")
                if (metadataFile.exists()) {
                    val metadataJson = metadataFile.readText()
                    json.decodeFromString<DocumentMetadata>(metadataJson)
                } else {
                    null
                }
            } catch (e: Exception) {
                throw DocumentStorageException(
                    "Failed to load metadata for document $documentId",
                    e
                )
            }
        }

    override suspend fun deleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, "$documentId.rcd")
            val metadataFile = File(metadataDir, "$documentId.json")

            if (file.exists()) {
                file.delete()
            }
            if (metadataFile.exists()) {
                metadataFile.delete()
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

    override suspend fun listDocumentsWithMetadata(): List<DocumentMetadata> =
        withContext(Dispatchers.IO) {
            try {
                val documentIds = listDocuments()
                documentIds.mapNotNull { documentId ->
                    loadDocumentMetadata(documentId)
                }
            } catch (e: Exception) {
                throw DocumentStorageException("Failed to list documents with metadata", e)
            }
        }

    override fun getDocumentsDirectory(): String = documentsDir.absolutePath

    override suspend fun getDocumentSize(documentId: String): Long? = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, "$documentId.rcd")
            if (file.exists()) {
                file.length()
            } else {
                null
            }
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun documentExists(documentId: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(documentsDir, "$documentId.rcd")
        file.exists()
    }

    private suspend fun saveMetadata(documentId: String, metadata: DocumentMetadata) =
        withContext(Dispatchers.IO) {
            try {
                val metadataFile = File(metadataDir, "$documentId.json")
                val metadataJson = json.encodeToString(metadata)
                metadataFile.writeText(metadataJson)
            } catch (e: Exception) {
                throw DocumentStorageException(
                    "Failed to save metadata for document $documentId",
                    e
                )
            }
        }

    private fun calculateChecksum(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Exception thrown when document storage operations fail
 */
class DocumentStorageException(message: String, cause: Throwable? = null) :
    Exception(message, cause)