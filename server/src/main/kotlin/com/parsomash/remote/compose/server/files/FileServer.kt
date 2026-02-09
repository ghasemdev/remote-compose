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
     * @return The document file contents with metadata
     * @throws DocumentNotFoundException if the document file doesn't exist
     */
    suspend fun serveDocument(documentId: String): DocumentWithMetadata

    /**
     * Lists all available document files.
     * @return List of document metadata for all available documents
     */
    suspend fun listDocuments(): List<DocumentMetadata>
}

/**
 * Document content with metadata including modification time.
 * This ensures the server always serves the latest version of files.
 */
data class DocumentWithMetadata(
    val documentId: String,
    val content: ByteArray,
    val lastModified: Long,
    val size: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentWithMetadata

        if (documentId != other.documentId) return false
        if (!content.contentEquals(other.content)) return false
        if (lastModified != other.lastModified) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentId.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
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
