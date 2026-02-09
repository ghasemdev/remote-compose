package com.parsomash.remote.compose.server.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AccessDeniedException

/**
 * Ktor-based file server for serving pre-generated Remote Compose documents.
 * This implementation has no Remote Compose dependencies and simply serves files.
 * 
 * Thread-safety: This implementation is designed to handle concurrent requests safely.
 * File reads are performed on the IO dispatcher and use immutable file references.
 * The semaphore limits concurrent file operations to prevent resource exhaustion.
 *
 * @param documentsDirectory The directory containing pre-generated document files
 * @param maxConcurrentReads Maximum number of concurrent file read operations (default: 100)
 */
class KtorFileServer(
    private val documentsDirectory: String,
    maxConcurrentReads: Int = 100
) : FileServer {
    private val logger = LoggerFactory.getLogger(KtorFileServer::class.java)
    
    // Semaphore to limit concurrent file operations and prevent resource exhaustion
    private val fileAccessSemaphore = Semaphore(maxConcurrentReads)

    private val documentsDir: File = File(documentsDirectory).apply {
        if (!exists()) {
            logger.info("Documents directory does not exist, creating: $absolutePath")
            val created = mkdirs()
            if (created) {
                logger.info("Successfully created documents directory: $absolutePath")
            } else {
                logger.error("Failed to create documents directory: $absolutePath")
                throw IOException("Failed to create documents directory: $absolutePath")
            }
        } else {
            logger.info("Using existing documents directory: $absolutePath")
        }
        
        if (!canRead()) {
            logger.error("Documents directory is not readable: $absolutePath")
            throw AccessDeniedException("Documents directory is not readable: $absolutePath")
        }
    }

    init {
        logger.info("KtorFileServer initialized with directory: ${documentsDir.absolutePath}")
        logger.info("Max concurrent reads: $maxConcurrentReads")
    }

    override suspend fun serveDocument(documentId: String): ByteArray {
        logger.debug("Request to serve document: $documentId")
        
        // Validate document ID to prevent path traversal attacks
        if (!isValidDocumentId(documentId)) {
            logger.warn("Invalid document ID requested: $documentId")
            throw IllegalArgumentException("Invalid document ID: $documentId")
        }
        
        return withContext(Dispatchers.IO) {
            fileAccessSemaphore.acquire()
            try {
                val file = findDocumentFile(documentId)
                
                if (!file.exists()) {
                    logger.warn("Document file not found: $documentId (path: ${file.absolutePath})")
                    throw DocumentNotFoundException(documentId)
                }
                
                if (!file.isFile) {
                    logger.warn("Document path is not a file: $documentId (path: ${file.absolutePath})")
                    throw DocumentNotFoundException(documentId)
                }
                
                if (!file.canRead()) {
                    logger.error("Document file is not readable: $documentId (path: ${file.absolutePath})")
                    throw IOException("Document file is not readable: $documentId")
                }
                
                val fileSize = file.length()
                logger.info("Serving document: $documentId (size: $fileSize bytes)")
                
                val bytes = try {
                    file.readBytes()
                } catch (e: IOException) {
                    logger.error("IO error reading document file: $documentId", e)
                    throw IOException("Failed to read document file: $documentId", e)
                }
                
                logger.debug("Successfully served document: $documentId ($fileSize bytes)")
                bytes
            } catch (e: DocumentNotFoundException) {
                // Re-throw DocumentNotFoundException as-is
                throw e
            } catch (e: IllegalArgumentException) {
                // Re-throw validation errors as-is
                throw e
            } catch (e: Exception) {
                logger.error("Unexpected error serving document: $documentId", e)
                throw IOException("Unexpected error serving document: $documentId", e)
            } finally {
                fileAccessSemaphore.release()
            }
        }
    }

    override suspend fun listDocuments(): List<DocumentMetadata> {
        logger.debug("Request to list all documents")
        
        return withContext(Dispatchers.IO) {
            fileAccessSemaphore.acquire()
            try {
                if (!documentsDir.exists()) {
                    logger.warn("Documents directory no longer exists: ${documentsDir.absolutePath}")
                    return@withContext emptyList()
                }
                
                if (!documentsDir.canRead()) {
                    logger.error("Documents directory is not readable: ${documentsDir.absolutePath}")
                    throw IOException("Documents directory is not readable")
                }
                
                val files = documentsDir.listFiles()
                
                if (files == null) {
                    logger.warn("Failed to list files in documents directory: ${documentsDir.absolutePath}")
                    return@withContext emptyList()
                }
                
                val documents = files
                    .filter { it.isFile && it.extension == "rcd" }
                    .mapNotNull { file ->
                        try {
                            DocumentMetadata(
                                id = file.nameWithoutExtension,
                                filename = file.name,
                                size = file.length(),
                                lastModified = file.lastModified()
                            )
                        } catch (e: Exception) {
                            logger.warn("Error reading metadata for file: ${file.name}", e)
                            null // Skip files that can't be read
                        }
                    }
                
                logger.info("Listed ${documents.size} documents from directory")
                documents
            } catch (e: Exception) {
                logger.error("Error listing documents", e)
                throw IOException("Failed to list documents", e)
            } finally {
                fileAccessSemaphore.release()
            }
        }
    }

    /**
     * Validates that a document ID is safe to use (prevents path traversal attacks).
     */
    private fun isValidDocumentId(documentId: String): Boolean {
        // Document ID should not contain path separators or special characters
        return documentId.isNotBlank() && 
               !documentId.contains("..") && 
               !documentId.contains("/") && 
               !documentId.contains("\\") &&
               documentId.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }

    private fun findDocumentFile(documentId: String): File {
        // Look for file with .rcd extension (Remote Compose Document)
        return File(documentsDir, "$documentId.rcd")
    }
}

/**
 * Response model for document list endpoint.
 */
@Serializable
data class DocumentListResponse(
    val documents: List<DocumentMetadata>
)

/**
 * Response model for error responses.
 */
@Serializable
data class ErrorResponse(
    val error: String
)
