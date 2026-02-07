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
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Sample Remote Compose content for demonstration
 */
@RemoteComposable
@Composable
fun SampleRemoteContent() {
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
fun GreetingCard(name: String = "World") {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
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
 * Sample showing how to use DocumentGenerator in a Composable
 */
@Composable
fun DocumentGeneratorSample(
    documentGenerator: DocumentGenerator,
    onDocumentGenerated: (RemoteDocument, ByteArray) -> Unit
) {
    // Generate a sample document
    documentGenerator.GenerateAndSaveDocument(
        documentId = "sample_content",
        content = { SampleRemoteContent() },
        onDocumentGenerated = onDocumentGenerated
    )
}
