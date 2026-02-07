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
import com.parsomash.remote.compose.document.DocumentGeneratorImpl
import com.parsomash.remote.compose.document.FileSystemManagerImpl
import kotlinx.coroutines.launch

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

    // Create DocumentGenerator with FileSystemManager
    val fileSystemManager = remember { FileSystemManagerImpl(context) }
    val documentGenerator = remember { DocumentGeneratorImpl(fileSystemManager) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Use the new DocumentGenerator to create and save documents
        documentGenerator.GenerateAndSaveDocument(
            documentId = "sample_greeting",
            content = { Greeting(modifier = RemoteModifier.fillMaxSize()) },
            onDocumentGenerated = { document, bytes ->
                println("Document generated: $document")
                if (documentState == null) {
                    documentState = document

                    // Save the document bytes to file system
                    coroutineScope.launch {
                        try {
                            documentGenerator.saveDocumentBytes("sample_greeting", bytes)
                            println("Document saved to file system")
                        } catch (e: Exception) {
                            println("Failed to save document: ${e.message}")
                        }
                    }
                }
            }
        )

        // Render the generated document
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
