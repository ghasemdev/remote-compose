package com.parsomash.remote.compose.server.files

import kotlinx.serialization.Serializable

/**
 * Interface for serving pre-generated Remote Compose document files.
 * This is a simple file server with no Remote Compose dependencies.
 */
interface FileServer {
    /**
     * Serves a document file by its ID.
     * @param documentId The unique identifier of the document
     * @return The document file contents as a byte array
     * @throws DocumentNotFoundException if the document file doesn't exist
     */
    suspend fun serveDocument(documentId: String): ByteArray

    /**
     * Lists all available document files.
     * @return List of document metadata for all available documents
     */
    suspend fun listDocuments(): List<DocumentMetadata>
}

/**
 * Metadata about a document file.
 */
@Serializable
data class DocumentMetadata(
    val id: String,
    val filename: String,
    val size: Long,
    val lastModified: Long
)

/**
 * Exception thrown when a requested document file is not found.
 */
class DocumentNotFoundException(documentId: String) : Exception(
    "Document not found: $documentId"
)
