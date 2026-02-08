@file:SuppressLint("RestrictedApi")

package com.parsomash.remote.compose.document

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Utility class for generating sample Remote Compose documents for testing
 *
 * Note: This class provides helper methods for working with the DocumentGenerationService.
 * Actual document generation must be done within a Composable context.
 */
class SampleDocumentGenerator(
    private val documentService: DocumentGenerationService
) {
    /**
     * Lists all generated sample documents
     */
    suspend fun listSamples(): List<DocumentMetadata> {
        return documentService.listGeneratedDocuments()
    }

    /**
     * Verifies that generated files are accessible
     */
    suspend fun verifyGeneratedFiles(): VerificationResult {
        val documents = documentService.listGeneratedDocuments()
        val results = mutableListOf<FileVerification>()

        documents.forEach { metadata ->
            val exists = documentService.documentExists(metadata.id)
            val loadable = try {
                documentService.loadDocument(metadata.id) != null
            } catch (e: Exception) {
                false
            }

            results.add(
                FileVerification(
                    documentId = metadata.id,
                    filePath = metadata.filePath,
                    exists = exists,
                    loadable = loadable,
                    fileSize = metadata.fileSize
                )
            )
        }

        return VerificationResult(
            totalDocuments = results.size,
            accessibleDocuments = results.count { it.exists && it.loadable },
            verifications = results
        )
    }

    data class FileVerification(
        val documentId: String,
        val filePath: String,
        val exists: Boolean,
        val loadable: Boolean,
        val fileSize: Long
    )

    data class VerificationResult(
        val totalDocuments: Int,
        val accessibleDocuments: Int,
        val verifications: List<FileVerification>
    ) {
        val allAccessible: Boolean
            get() = totalDocuments == accessibleDocuments
    }
}

/**
 * Composable functions for generating sample documents
 */
object SampleDocuments {
    /**
     * Generates a simple greeting document
     */
    @Composable
    fun GenerateGreetingSample(
        documentService: DocumentGenerationService,
        onSuccess: (RemoteDocument, DocumentMetadata) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) {
        documentService.GenerateAndPersistDocument(
            documentId = "sample_greeting",
            title = "Simple Greeting",
            description = "A basic greeting card demonstrating Remote Compose text rendering",
            tags = listOf("sample", "greeting", "basic"),
            content = { SimpleGreeting() },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Generates a comprehensive content sample
     */
    @Composable
    fun GenerateContentSample(
        documentService: DocumentGenerationService,
        onSuccess: (RemoteDocument, DocumentMetadata) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) {
        documentService.GenerateAndPersistDocument(
            documentId = "sample_content",
            title = "Sample Content",
            description = "Comprehensive sample showing various Remote Compose components",
            tags = listOf("sample", "comprehensive", "demo"),
            content = { SampleRemoteContent() },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Generates a layout demonstration sample
     */
    @Composable
    fun GenerateLayoutSample(
        documentService: DocumentGenerationService,
        onSuccess: (RemoteDocument, DocumentMetadata) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) {
        documentService.GenerateAndPersistDocument(
            documentId = "sample_layout",
            title = "Layout Demo",
            description = "Demonstrates Remote Compose layout capabilities",
            tags = listOf("sample", "layout", "demo"),
            content = { LayoutDemoContent() },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    @RemoteComposable
    @Composable
    private fun SimpleGreeting() {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center
        ) {
            RemoteColumn(
                horizontalAlignment = RemoteAlignment.CenterHorizontally
            ) {
                RemoteText(text = "Hello, Remote Compose!")
                RemoteText(text = "This is a simple greeting")
            }
        }
    }

    /**
     * Sample Remote Compose content for demonstration
     */
    @RemoteComposable
    @Composable
    private fun SampleRemoteContent() {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center
        ) {
            RemoteText(text = "Welcome to Remote Compose!")
            RemoteText(text = "This content was generated using DocumentGenerator")
            RemoteText(text = "and can be served to remote clients")

            GreetingCard()
        }
    }

    /**
     * Another sample showing a simple greeting card
     */
    @RemoteComposable
    @Composable
    private fun GreetingCard(name: String = "World") {
        RemoteBox(
            modifier = RemoteModifier.fillMaxWidth(),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center
        ) {
            RemoteColumn(
                horizontalAlignment = RemoteAlignment.CenterHorizontally
            ) {
                RemoteText(text = "Hello, $name!")
                RemoteText(text = "Generated with Remote Compose")
            }
        }
    }

    /**
     * Sample showing a layout demonstration
     */
    @RemoteComposable
    @Composable
    private fun LayoutDemoContent() {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = RemoteAlignment.Start,
            verticalArrangement = RemoteArrangement.SpaceBetween
        ) {
            RemoteText(text = "Layout Demo")
            RemoteText(text = "Top Section")

            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center
            ) {
                RemoteText(text = "Center Content")
            }

            RemoteText(text = "Bottom Section")
        }
    }
}
