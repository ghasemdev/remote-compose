@file:SuppressLint("RestrictedApi")

package com.parsomash.remote.compose.document

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service that integrates DocumentGenerator with FileSystemManager
 * to provide a complete document generation and storage workflow
 */
class DocumentGenerationService(
    private val documentGenerator: DocumentGenerator,
    private val fileSystemManager: FileSystemManager,
    private val coroutineScope: CoroutineScope
) {
    /**
     * Generates a document and automatically saves it to the file system
     *
     * @param documentId Unique identifier for the document
     * @param title Human-readable title for the document
     * @param description Optional description of the document
     * @param tags Optional tags for categorization
     * @param content Composable content to be converted to a Remote Document
     * @param onSuccess Callback invoked when document is successfully generated and saved
     * @param onError Callback invoked when an error occurs
     */
    @Composable
    fun GenerateAndPersistDocument(
        documentId: String,
        title: String = documentId,
        description: String = "",
        tags: List<String> = emptyList(),
        content: @RemoteComposable @Composable () -> Unit,
        onSuccess: (RemoteDocument, DocumentMetadata) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) {
        documentGenerator.GenerateAndSaveDocument(
            documentId = documentId,
            content = content,
            onDocumentGenerated = { document, bytes ->
                // Save document with metadata in a coroutine
                coroutineScope.launch {
                    try {
                        val currentTime = System.currentTimeMillis()
                        val metadata = DocumentMetadata(
                            id = documentId,
                            title = title,
                            description = description,
                            createdAt = currentTime,
                            updatedAt = currentTime,
                            tags = tags,
                            filePath = "", // Will be set by FileSystemManager
                            fileSize = bytes.size.toLong()
                        )

                        fileSystemManager.saveDocumentWithMetadata(
                            documentId = documentId,
                            bytes = bytes,
                            metadata = metadata
                        )

                        // Load the saved metadata to get the complete file path
                        val savedMetadata = fileSystemManager.loadDocumentMetadata(documentId)
                        if (savedMetadata != null) {
                            onSuccess(document, savedMetadata)
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }
        )
    }

    /**
     * Generates multiple sample documents for testing
     *
     * @param onProgress Callback invoked for each document generated
     * @param onComplete Callback invoked when all documents are generated
     */
    suspend fun generateSampleDocuments(
        onProgress: (String, Int, Int) -> Unit = { _, _, _ -> },
        onComplete: (List<DocumentMetadata>) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val generatedMetadata = mutableListOf<DocumentMetadata>()
        val samples = getSampleDocumentSpecs()

        samples.forEachIndexed { index, spec ->
            try {
                onProgress(spec.id, index + 1, samples.size)

                // Note: This is a simplified version for background generation
                // In a real scenario, you'd need to use a Composable context
                // For now, we'll just create placeholder metadata
                val currentTime = System.currentTimeMillis()
                val metadata = DocumentMetadata(
                    id = spec.id,
                    title = spec.title,
                    description = spec.description,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    tags = spec.tags,
                    filePath = "",
                    fileSize = 0L
                )
                generatedMetadata.add(metadata)
            } catch (e: Exception) {
                // Log error but continue with other documents
                println("Failed to generate sample document ${spec.id}: ${e.message}")
            }
        }

        onComplete(generatedMetadata)
    }

    /**
     * Lists all generated documents with their metadata
     */
    suspend fun listGeneratedDocuments(): List<DocumentMetadata> {
        return fileSystemManager.listDocumentsWithMetadata()
    }

    /**
     * Loads a previously generated document
     */
    suspend fun loadDocument(documentId: String): RemoteDocument? {
        val bytes = fileSystemManager.loadDocument(documentId) ?: return null
        return RemoteDocument(bytes)
    }

    /**
     * Deletes a generated document
     */
    suspend fun deleteDocument(documentId: String) {
        fileSystemManager.deleteDocument(documentId)
    }

    /**
     * Checks if a document exists
     */
    suspend fun documentExists(documentId: String): Boolean {
        return fileSystemManager.documentExists(documentId)
    }

    /**
     * Gets the documents directory path
     */
    fun getDocumentsDirectory(): String {
        return fileSystemManager.getDocumentsDirectory()
    }

    private data class SampleDocumentSpec(
        val id: String,
        val title: String,
        val description: String,
        val tags: List<String>
    )

    private fun getSampleDocumentSpecs(): List<SampleDocumentSpec> {
        return listOf(
            SampleDocumentSpec(
                id = "sample_greeting",
                title = "Simple Greeting",
                description = "A basic greeting card demonstrating Remote Compose text rendering",
                tags = listOf("sample", "greeting", "basic")
            ),
            SampleDocumentSpec(
                id = "sample_content",
                title = "Sample Content",
                description = "Comprehensive sample showing various Remote Compose components",
                tags = listOf("sample", "comprehensive", "demo")
            ),
            SampleDocumentSpec(
                id = "sample_layout",
                title = "Layout Demo",
                description = "Demonstrates Remote Compose layout capabilities",
                tags = listOf("sample", "layout", "demo")
            )
        )
    }
}

/**
 * Factory function to create DocumentGenerationService
 */
fun createDocumentGenerationService(
    context: Context,
    coroutineScope: CoroutineScope
): DocumentGenerationService {
    val fileSystemManager = FileSystemManagerImpl(context)
    val documentGenerator = DocumentGeneratorImpl(fileSystemManager)
    return DocumentGenerationService(documentGenerator, fileSystemManager, coroutineScope)
}
