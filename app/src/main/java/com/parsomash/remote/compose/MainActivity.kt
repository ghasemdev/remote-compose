package com.parsomash.remote.compose

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.remote.creation.compose.capture.RememberRemoteDocumentInline
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview

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
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
        RememberRemoteDocumentInline(
            onDocument = { doc ->
                println("Document generated: $doc")
                if (documentState == null) {
                    // Generate seems to get called again with a partial document
                    // Essentially re-recording but with existing state, so document is incomplete
                    documentState = RemoteDocument(doc.buffer.buffer.cloneBytes())
                }
            },
        ) {
            Greeting(modifier = RemoteModifier.fillMaxSize())
        }

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
