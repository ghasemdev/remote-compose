package com.parsomash.remote.compose

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import com.parsomash.remote.compose.document.DocumentMetadata
import com.parsomash.remote.compose.document.createDocumentGenerationService
import com.parsomash.remote.compose.document.createDocumentPlayerService
import com.parsomash.remote.compose.document.createDocumentRenderingService
import com.parsomash.remote.compose.network.ClientConfig
import com.parsomash.remote.compose.network.KtorClientManagerImpl

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Main(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
@Suppress("RestrictedApiAndroidX")
fun Main(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }
    var documentMetadata by remember { mutableStateOf<DocumentMetadata?>(null) }
    var useNetworkFetching by remember { mutableStateOf(false) }

    // Create DocumentGenerationService for integrated document generation and storage
    val documentService = remember {
        createDocumentGenerationService(context = context, coroutineScope = coroutineScope)
    }

    // Create integrated document fetching and rendering service
    val ktorClientManager = remember {
        KtorClientManagerImpl(
            config = ClientConfig(
                baseUrl = "http://localhost:8080/",
                enableLogging = true
            )
        )
    }

    val documentPlayerService = remember { createDocumentPlayerService() }

    val documentRenderingService = remember {
        createDocumentRenderingService(
            ktorClientManager = ktorClientManager,
            documentPlayerService = documentPlayerService
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!useNetworkFetching) {
            // First generate and save the document locally
            documentService.GenerateAndPersistDocument(
                documentId = "sample_greeting",
                title = "Sample Greeting Card",
                description = "A simple greeting demonstrating Remote Compose integration",
                tags = listOf("sample", "greeting", "demo"),
                content = { Greeting(modifier = RemoteModifier.fillMaxSize()) },
                onSuccess = { document, metadata ->
                    if (documentState == null) {
                        documentState = document
                        documentMetadata = metadata
                        println("Document generated and saved successfully:")
                        println("  ID: ${metadata.id}")
                        println("  Title: ${metadata.title}")
                        println("  File: ${metadata.filePath}")
                        println("  Size: ${metadata.fileSize} bytes")
                        println("  Checksum: ${metadata.checksum}")

                        // Switch to network fetching mode after generation
                        useNetworkFetching = true
                    }
                },
                onError = { error ->
                    println("Failed to generate or save document: ${error.message}")
                    error.printStackTrace()
                }
            )

            // Render the locally generated document
            if (documentState != null) {
                val windowInfo = LocalWindowInfo.current
                RemoteDocumentPlayer(
                    document = documentState!!.document,
                    windowInfo.containerSize.width,
                    windowInfo.containerSize.height,
                    modifier = modifier.fillMaxSize(),
                    0,
                    onNamedAction = { _, _, _ -> },
                )
            }
        } else {
            // Demonstrate integrated document fetching and rendering
            documentRenderingService.FetchAndRenderDocument(
                documentId = "sample_greeting",
                modifier = modifier,
                onError = { error ->
                    println("Document rendering error: ${error.message}")
                    error.printStackTrace()
                    // Fallback to local rendering if network fails
                    useNetworkFetching = false
                }
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
fun Greeting(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(
        modifier = modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        RemoteText(text = "Hello world!")
    }
}

@SuppressLint("RestrictedApi")
@Suppress("RestrictedApiAndroidX")
@Preview
@Composable
fun GreetingPreview() {
    RemotePreview { Greeting() }
}
