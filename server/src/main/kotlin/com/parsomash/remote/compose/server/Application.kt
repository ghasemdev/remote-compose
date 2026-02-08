package com.parsomash.remote.compose.server

import com.parsomash.remote.compose.server.files.KtorFileServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    // Get documents directory from environment or use default
    val documentsDir = System.getenv("DOCUMENTS_DIR")
        ?: "${System.getProperty("user.home")}/remote-compose-documents"
    val fileServer = KtorFileServer(documentsDir)

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, port = port) {
        fileServerModule(fileServer)
    }.start(wait = true)
}
