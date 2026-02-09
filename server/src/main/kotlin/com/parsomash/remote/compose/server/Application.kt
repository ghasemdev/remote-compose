package com.parsomash.remote.compose.server

import com.parsomash.remote.compose.server.files.KtorFileServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    logger.info("Starting Remote Compose File Server")
    
    // Get documents directory from environment or use default
    val documentsDir = System.getenv("DOCUMENTS_DIR")
        ?: "${System.getProperty("user.home")}/remote-compose-documents"
    
    logger.info("Documents directory: $documentsDir")
    
    val fileServer = try {
        KtorFileServer(documentsDir)
    } catch (e: Exception) {
        logger.error("Failed to initialize file server", e)
        throw e
    }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Server will listen on port: $port")

    try {
        embeddedServer(Netty, port = port) {
            fileServerModule(fileServer)
        }.start(wait = true)
        
        logger.info("Server started successfully on port $port")
    } catch (e: Exception) {
        logger.error("Failed to start server", e)
        throw e
    }
}
