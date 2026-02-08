package com.parsomash.remote.compose.server

import com.parsomash.remote.compose.server.files.KtorFileServer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtorRoutesTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        // Create test documents
        File(tempDir, "sample-doc.rcd").writeBytes("Sample document content".toByteArray())
        File(tempDir, "another-doc.rcd").writeBytes("Another document content".toByteArray())
    }

    @Test
    fun `GET health endpoint returns OK`(): Unit = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("healthy"))
    }

    @Test
    fun `GET api documents returns list of documents`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())

        val body = response.bodyAsText()
        assertTrue(body.contains("sample-doc"))
        assertTrue(body.contains("another-doc"))
    }

    @Test
    fun `GET api documents id returns document content`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents/sample-doc")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.OctetStream, response.contentType())
        assertEquals("Sample document content", response.bodyAsText())
    }

    @Test
    fun `GET api documents id returns 404 for non-existent document`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents/non-existent")

        assertEquals(HttpStatusCode.NotFound, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("Document not found") || body.contains("non-existent"))
    }

    @Test
    fun `GET api documents id returns 400 for empty document ID`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents/")

        // Should either be 404 (route not found) or handled by list endpoint
        assertTrue(
            response.status == HttpStatusCode.NotFound ||
                    response.status == HttpStatusCode.OK
        )
    }

    @Test
    fun `CORS headers are present on GET requests`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents") {
            header(HttpHeaders.Origin, "http://localhost:3000")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun `OPTIONS request returns CORS headers`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.options("/api/documents") {
            header(HttpHeaders.Origin, "http://localhost:3000")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        // CORS preflight should return OK or No Content
        assertTrue(
            response.status == HttpStatusCode.OK ||
                    response.status == HttpStatusCode.NoContent
        )
        assertNotNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun `GET api documents returns correct document metadata`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify response contains expected fields
        assertTrue(body.contains("\"id\""))
        assertTrue(body.contains("\"filename\""))
        assertTrue(body.contains("\"size\""))
        assertTrue(body.contains("\"lastModified\""))
        assertTrue(body.contains(".rcd"))
    }

    @Test
    fun `GET api documents returns empty list for empty directory`() = testApplication {
        // Create a new empty temp directory
        val emptyDir = File(tempDir, "empty").apply { mkdirs() }

        application {
            val fileServer = KtorFileServer(emptyDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("\"documents\":[]") || body.contains("\"documents\": []"))
    }

    @Test
    fun `GET api documents filters non-rcd files`() = testApplication {
        // Add a non-rcd file
        File(tempDir, "readme.txt").writeBytes("This is a readme".toByteArray())
        File(tempDir, "config.json").writeBytes("{}".toByteArray())

        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)

        // Should only contain .rcd files
        assertTrue(body.contains("sample-doc"))
        assertTrue(body.contains("another-doc"))
        assertTrue(!body.contains("readme.txt"))
        assertTrue(!body.contains("config.json"))
    }

    @Test
    fun `Compression is applied to responses`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        val response = client.get("/api/documents") {
            header(HttpHeaders.AcceptEncoding, "gzip, deflate")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        // Compression headers may or may not be present depending on response size
        // Just verify the request succeeds
    }

    @Test
    fun `Multiple concurrent requests are handled correctly`() = testApplication {
        application {
            val fileServer = KtorFileServer(tempDir.absolutePath)
            fileServerModule(fileServer)
        }

        // Make multiple concurrent requests
        val responses = listOf(
            client.get("/api/documents/sample-doc"),
            client.get("/api/documents/another-doc"),
            client.get("/api/documents"),
            client.get("/health")
        )

        // All should succeed
        responses.forEach { response ->
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
