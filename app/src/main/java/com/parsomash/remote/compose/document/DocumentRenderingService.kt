package com.parsomash.remote.compose.document

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parsomash.remote.compose.network.HttpRequestException
import com.parsomash.remote.compose.network.KtorClientManager
import kotlinx.coroutines.CancellationException

/**
 * Service that integrates document fetching with rendering
 * Connects KtorClientManager with DocumentPlayerService to provide
 * seamless document retrieval and rendering capabilities
 */
interface DocumentRenderingService {
    /**
     * Fetches and renders a remote document by ID
     *
     * @param documentId The ID of the document to fetch and render
     * @param modifier Modifier to apply to the rendered content
     * @param onError Callback invoked when fetching or rendering fails
     */
    @Composable
    fun FetchAndRenderDocument(
        documentId: String,
        modifier: Modifier = Modifier,
        onError: (DocumentRenderingError) -> Unit = {}
    )

    /**
     * Fetches a document by ID and returns the deserialized RemoteDocument
     *
     * @param documentId The ID of the document to fetch
     * @return The deserialized RemoteDocument
     * @throws DocumentRenderingError if fetching or deserialization fails
     */
    @SuppressLint("RestrictedApi")
    suspend fun fetchDocument(documentId: String): RemoteDocument

    /**
     * Renders a RemoteDocument using the DocumentPlayerService
     *
     * @param document The RemoteDocument to render
     * @param modifier Modifier to apply to the rendered content
     * @param onError Callback invoked when rendering fails
     */
    @Composable
    fun renderDocument(
        @SuppressLint("RestrictedApi") document: RemoteDocument,
        modifier: Modifier = Modifier,
        onError: (DocumentRenderingError) -> Unit = {}
    )
}

/**
 * Error types for document rendering operations
 */
sealed class DocumentRenderingError(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    /**
     * Network-related errors during document fetching
     */
    data class NetworkError(
        val statusCode: Int? = null,
        override val cause: Throwable
    ) : DocumentRenderingError(
        "Network error during document fetch${statusCode?.let { " (HTTP $it)" } ?: ""}: ${cause.message}",
        cause
    )

    /**
     * Document deserialization errors
     */
    data class DeserializationError(
        val documentId: String,
        override val cause: Throwable
    ) : DocumentRenderingError(
        "Failed to deserialize document '$documentId': ${cause.message}",
        cause
    )

    /**
     * Document rendering errors
     */
    data class RenderingError(
        val documentId: String? = null,
        override val cause: Throwable
    ) : DocumentRenderingError(
        "Failed to render document${documentId?.let { " '$it'" } ?: ""}: ${cause.message}",
        cause
    )

    /**
     * General operation errors
     */
    data class OperationError(
        val operation: String,
        override val cause: Throwable
    ) : DocumentRenderingError(
        "Failed to $operation: ${cause.message}",
        cause
    )
}

/**
 * Implementation of DocumentRenderingService that integrates
 * KtorClientManager with DocumentPlayerService
 */
class DocumentRenderingServiceImpl(
    private val ktorClientManager: KtorClientManager,
    private val documentPlayerService: DocumentPlayerService
) : DocumentRenderingService {
    @Composable
    override fun FetchAndRenderDocument(
        documentId: String,
        modifier: Modifier,
        onError: (DocumentRenderingError) -> Unit
    ) {
        var document by remember(documentId) { mutableStateOf<RemoteDocument?>(null) }
        var error by remember(documentId) { mutableStateOf<DocumentRenderingError?>(null) }
        var isLoading by remember(documentId) { mutableStateOf(true) }

        // Fetch document when documentId changes
        LaunchedEffect(documentId) {
            try {
                isLoading = true
                error = null
                document = fetchDocument(documentId)
            } catch (e: DocumentRenderingError) {
                error = e
                onError(e)
            } catch (e: Exception) {
                val wrappedError = DocumentRenderingError.OperationError("fetch document", e)
                error = wrappedError
                onError(wrappedError)
            } finally {
                isLoading = false
            }
        }

        // Render document or show error state
        when {
            isLoading -> {
                // Show loading state - could be customized
                LoadingDocumentView(modifier = modifier)
            }

            error != null -> {
                // Show error state - could be customized
                ErrorDocumentView(
                    error = error!!,
                    modifier = modifier
                )
            }

            document != null -> {
                // Render the document
                renderDocument(
                    document = document!!,
                    modifier = modifier,
                    onError = onError
                )
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override suspend fun fetchDocument(documentId: String): RemoteDocument {
        return try {
            // Fetch document bytes from server
            val documentBytes = ktorClientManager.fetchDocument(documentId)

            // Deserialize bytes into RemoteDocument
            deserializeDocument(documentId, documentBytes)
        } catch (e: HttpRequestException) {
            throw DocumentRenderingError.NetworkError(
                statusCode = e.statusCode,
                cause = e
            )
        } catch (e: CancellationException) {
            // Re-throw cancellation exceptions to preserve coroutine cancellation
            throw e
        } catch (e: Exception) {
            throw DocumentRenderingError.NetworkError(cause = e)
        }
    }

    @Composable
    override fun renderDocument(
        @SuppressLint("RestrictedApi") document: RemoteDocument,
        modifier: Modifier,
        onError: (DocumentRenderingError) -> Unit
    ) {
        documentPlayerService.RenderDocument(
            document = document,
            modifier = modifier,
            onError = { throwable ->
                val renderingError = DocumentRenderingError.RenderingError(
                    cause = throwable
                )
                onError(renderingError)
            }
        )
    }

    /**
     * Deserializes document bytes into a RemoteDocument
     *
     * @param documentId The ID of the document (for error reporting)
     * @param bytes The document bytes to deserialize
     * @return The deserialized RemoteDocument
     * @throws DocumentRenderingError.DeserializationError if deserialization fails
     */
    @SuppressLint("RestrictedApi")
    private fun deserializeDocument(documentId: String, bytes: ByteArray): RemoteDocument {
        return try {
            // Use RemoteDocument constructor to deserialize bytes
            // This follows the androidx.compose.remote library pattern
            RemoteDocument(bytes)
        } catch (e: Exception) {
            throw DocumentRenderingError.DeserializationError(
                documentId = documentId,
                cause = e
            )
        }
    }
}

/**
 * Composable that shows a loading state while fetching documents
 */
@Composable
private fun LoadingDocumentView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Composable that shows an error state when document operations fail
 */
@Composable
private fun ErrorDocumentView(
    error: DocumentRenderingError,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
//            Icon(
//                imageVector = Icons.Default.Error,
//                contentDescription = "Error",
//                tint = MaterialTheme.colorScheme.error
//            )
            Spacer(
                modifier = modifier.height(8.dp)
            )
            Text(
                text = "Failed to load document",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(
                modifier = modifier.height(4.dp)
            )
            Text(
                text = error.message ?: "Unknown error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Factory function to create DocumentRenderingService
 */
fun createDocumentRenderingService(
    ktorClientManager: KtorClientManager,
    documentPlayerService: DocumentPlayerService
): DocumentRenderingService {
    return DocumentRenderingServiceImpl(
        ktorClientManager = ktorClientManager,
        documentPlayerService = documentPlayerService
    )
}