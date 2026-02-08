package com.parsomash.remote.compose.server.api

import com.parsomash.remote.compose.server.files.DocumentListResponse
import com.parsomash.remote.compose.server.files.DocumentNotFoundException
import com.parsomash.remote.compose.server.files.ErrorResponse
import com.parsomash.remote.compose.server.files.FileServer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routes")

fun Routing.configureRoutes(fileServer: FileServer) {
    route("/api/documents") {
        // GET /api/documents - List all documents
        get {
            try {
                val documents = fileServer.listDocuments()
                call.respond(HttpStatusCode.OK, DocumentListResponse(documents))
            } catch (e: Exception) {
                logger.error("Error listing documents", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to list documents: ${e.message}")
                )
            }
        }

        // GET /api/documents/{id} - Get specific document
        get("/{id}") {
            val documentId = call.parameters["id"]
            if (documentId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Document ID is required")
                )
                return@get
            }

            try {
                val documentBytes = fileServer.serveDocument(documentId)
                call.respondBytes(
                    bytes = documentBytes,
                    contentType = ContentType.Application.OctetStream,
                    status = HttpStatusCode.OK
                )
            } catch (e: DocumentNotFoundException) {
                logger.warn("Document not found: $documentId")
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(e.message ?: "Document not found")
                )
            } catch (e: Exception) {
                logger.error("Error serving document: $documentId", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to serve document: ${e.message}")
                )
            }
        }
    }

    // Health check endpoint
    get("/health") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
    }
}
