@file:SuppressLint("RestrictedApi")

package com.parsomash.remote.compose.document

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Interface for rendering Remote Compose documents using androidx.compose.remote.player APIs
 */
interface DocumentPlayerService {
    /**
     * Renders a Remote Compose document using the remote player APIs
     *
     * @param document The RemoteDocument to render
     * @param modifier Modifier to apply to the rendered content
     * @param onError Callback invoked when rendering fails
     */
    @Composable
    fun RenderDocument(
        document: RemoteDocument,
        modifier: Modifier = Modifier,
        onError: (Throwable) -> Unit = {}
    )

    /**
     * Preloads a document for faster rendering
     *
     * @param document The RemoteDocument to preload
     */
    fun preloadDocument(document: RemoteDocument)

    /**
     * Gets the capabilities supported by the document player
     *
     * @return Set of supported remote capabilities
     */
    fun getCapabilities(): Set<RemoteCapability>
}

/**
 * Represents capabilities supported by the Remote Compose player
 */
enum class RemoteCapability {
    TEXT,
    LAYOUT,
    IMAGES,
    BUTTONS,
    EVENTS,
    ANIMATIONS,
    CUSTOM_COMPONENTS
}

/**
 * Implementation of DocumentPlayerService using androidx.compose.remote.player APIs
 *
 * This class uses RemoteDocumentPlayer to render Remote Compose documents
 * according to the library specifications. No wrapper functions are needed
 * as the library provides direct rendering capabilities.
 */
class DocumentPlayerServiceImpl : DocumentPlayerService {

    @SuppressLint("RestrictedApi")
    @Composable
    @Suppress("RestrictedApiAndroidX")
    override fun RenderDocument(
        document: RemoteDocument,
        modifier: Modifier,
        onError: (Throwable) -> Unit
    ) {
        var renderError by remember(document) { mutableStateOf<Throwable?>(null) }

        // Check if there's a rendering error to display
        if (renderError != null) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Failed to render document: ${renderError?.message ?: "Unknown error"}"
                )
            }
            return
        }

        // Get window information for proper sizing
        val windowInfo = LocalWindowInfo.current
        val width = windowInfo.containerSize.width
        val height = windowInfo.containerSize.height

        // Use RemoteDocumentPlayer to render the document
        // This is the official androidx.compose.remote.player API for rendering
        // Error handling is done through the onNamedAction callback and state management
        RemoteDocumentPlayer(
            document = document.document,
            documentWidth = width,
            documentHeight = height,
            modifier = modifier.fillMaxSize(),
            debugMode = 0, // depth parameter
            onNamedAction = { actionName, parameters, continuation ->
                // Handle named actions from remote components
                // This allows remote UI to trigger actions in the host app
                try {
                    // Parameters is Any? so we need to safely cast it
                    @Suppress("UNCHECKED_CAST")
                    val paramMap = parameters as? Map<String, Any> ?: emptyMap()
                    handleNamedAction(actionName, paramMap, continuation)
                } catch (e: Exception) {
                    // Capture action handling errors
                    renderError = e
                    onError(e)
                }
            }
        )
    }

    override fun preloadDocument(document: RemoteDocument) {
        // Preloading can be implemented by parsing the document structure
        // without rendering it. For now, this is a placeholder for future optimization.
        // The RemoteDocument is already deserialized when created, so the main
        // parsing work is done. Additional preloading could include:
        // - Prefetching remote resources
        // - Validating document structure
        // - Caching component metadata
    }

    override fun getCapabilities(): Set<RemoteCapability> {
        // Return the set of capabilities supported by this player implementation
        // Based on androidx.compose.remote.player capabilities
        return setOf(
            RemoteCapability.TEXT,
            RemoteCapability.LAYOUT,
            RemoteCapability.IMAGES,
            RemoteCapability.BUTTONS,
            RemoteCapability.EVENTS,
            RemoteCapability.ANIMATIONS,
            RemoteCapability.CUSTOM_COMPONENTS
        )
    }

    /**
     * Handles named actions triggered by remote components
     *
     * @param actionName The name of the action
     * @param parameters Parameters passed with the action
     * @param continuation Continuation to resume after handling the action
     */
    private fun handleNamedAction(
        actionName: String,
        parameters: Map<String, Any>,
        continuation: Any?
    ) {
        // Handle different action types
        when (actionName) {
            "navigation" -> handleNavigationAction(parameters)
            "api_call" -> handleApiCallAction(parameters)
            "state_update" -> handleStateUpdateAction(parameters)
            else -> handleCustomAction(actionName, parameters)
        }

        // Resume continuation if provided
        // This allows the remote component to continue execution after the action
    }

    private fun handleNavigationAction(parameters: Map<String, Any>) {
        // Handle navigation actions from remote UI
        // Example: navigate to a different screen
        val destination = parameters["destination"] as? String
        println("Navigation action to: $destination")
    }

    private fun handleApiCallAction(parameters: Map<String, Any>) {
        // Handle API call actions from remote UI
        // Example: trigger a network request
        val endpoint = parameters["endpoint"] as? String
        println("API call action to: $endpoint")
    }

    private fun handleStateUpdateAction(parameters: Map<String, Any>) {
        // Handle state update actions from remote UI
        // Example: update local app state
        val key = parameters["key"] as? String
        val value = parameters["value"]
        println("State update action: $key = $value")
    }

    private fun handleCustomAction(actionName: String, parameters: Map<String, Any>) {
        // Handle custom actions defined by the application
        println("Custom action: $actionName with parameters: $parameters")
    }
}

/**
 * Factory function to create DocumentPlayerService
 */
fun createDocumentPlayerService(): DocumentPlayerService {
    return DocumentPlayerServiceImpl()
}
