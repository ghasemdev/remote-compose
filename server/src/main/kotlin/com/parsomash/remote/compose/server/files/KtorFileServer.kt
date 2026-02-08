package com.parsomash.remote.compose.server.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Ktor-based file server for serving pre-generated Remote Compose documents.
 * This implementation has no Remote Compose dependencies and simply serves files.
 *
 * @param documentsDirectory The directory containing pre-generated document files
 */
class KtorFileServer(
    private val documentsDirectory: String
) : FileServer {
    private val logger = LoggerFactory.getLogger(KtorFileServer::class.java)

    private val documentsDir: File = File(documentsDirectory).apply {
        if (!exists()) {
            mkdirs()
            logger.info("Created documents directory: $absolutePath")
        }
    }

    override suspend fun serveDocument(documentId: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val file = findDocumentFile(documentId)
            if (!file.exists() || !file.isFile) {
                throw DocumentNotFoundException(documentId)
            }
            logger.debug("Serving document: $documentId (${file.length()} bytes)")
            file.readBytes()
        }
    }

    override suspend fun listDocuments(): List<DocumentMetadata> {
        return withContext(Dispatchers.IO) {
            documentsDir.listFiles()
                ?.filter { it.isFile && it.extension == "rcd" }
                ?.map { file ->
                    DocumentMetadata(
                        id = file.nameWithoutExtension,
                        filename = file.name,
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                } ?: emptyList()
        }
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
