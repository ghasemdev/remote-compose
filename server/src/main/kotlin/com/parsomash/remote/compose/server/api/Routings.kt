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
import java.io.IOException

private val logger = LoggerFactory.getLogger("Routes")

fun Routing.configureRoutes(fileServer: FileServer) {
    route("/api/documents") {
        // GET /api/documents - List all documents
        get {
            val startTime = System.currentTimeMillis()
            logger.info("Received request to list all documents")
            
            try {
                val documents = fileServer.listDocuments()
                val duration = System.currentTimeMillis() - startTime
                
                logger.info("Successfully listed ${documents.size} documents (took ${duration}ms)")
                call.respond(HttpStatusCode.OK, DocumentListResponse(documents))
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                logger.error("IO error listing documents (took ${duration}ms)", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to list documents: ${e.message}")
                )
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                logger.error("Unexpected error listing documents (took ${duration}ms)", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to list documents: ${e.message}")
                )
            }
        }

        // GET /api/documents/{id} - Get specific document
        get("/{id}") {
            val startTime = System.currentTimeMillis()
            val documentId = call.parameters["id"]
            
            if (documentId.isNullOrBlank()) {
                logger.warn("Request with missing or blank document ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Document ID is required")
                )
                return@get
            }

            logger.info("Received request for document: $documentId")

            try {
                val documentBytes = fileServer.serveDocument(documentId)
                val duration = System.currentTimeMillis() - startTime
                
                logger.info("Successfully served document: $documentId (${documentBytes.size} bytes, took ${duration}ms)")
                call.respondBytes(
                    bytes = documentBytes,
                    contentType = ContentType.Application.OctetStream,
                    status = HttpStatusCode.OK
                )
            } catch (e: DocumentNotFoundException) {
                val duration = System.currentTimeMillis() - startTime
                logger.warn("Document not found: $documentId (took ${duration}ms)")
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(e.message ?: "Document not found")
                )
            } catch (e: IllegalArgumentException) {
                val duration = System.currentTimeMillis() - startTime
                logger.warn("Invalid document ID: $documentId (took ${duration}ms)", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(e.message ?: "Invalid document ID")
                )
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                logger.error("IO error serving document: $documentId (took ${duration}ms)", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to serve document: ${e.message}")
                )
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                logger.error("Unexpected error serving document: $documentId (took ${duration}ms)", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to serve document: ${e.message}")
                )
            }
        }
    }

    // Health check endpoint
    get("/health") {
        logger.debug("Health check request received")
        call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
    }
}
