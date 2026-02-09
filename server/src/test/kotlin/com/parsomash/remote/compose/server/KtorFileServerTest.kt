package com.parsomash.remote.compose.server

import com.parsomash.remote.compose.server.files.DocumentNotFoundException
import com.parsomash.remote.compose.server.files.KtorFileServer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KtorFileServerTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var fileServer: KtorFileServer

    @BeforeEach
    fun setup() {
        fileServer = KtorFileServer(tempDir.absolutePath)
    }

    @Test
    fun `test serve document returns file contents`() = runBlocking {
        // Create a test document file
        val documentId = "test-doc"
        val content = "Test document content".toByteArray()
        File(tempDir, "$documentId.rcd").writeBytes(content)

        // Serve the document
        val result = fileServer.serveDocument(documentId)

        // Verify
        assertContentEquals(content, result)
    }

    @Test
    fun `test serve document throws exception for non-existent file`() {
        runBlocking {
            // Try to serve a non-existent document
            assertFailsWith<DocumentNotFoundException> {
                fileServer.serveDocument("non-existent")
            }
        }
    }

    @Test
    fun `test list documents returns all rcd files`() = runBlocking {
        // Create multiple test documents
        File(tempDir, "doc1.rcd").writeBytes("content1".toByteArray())
        File(tempDir, "doc2.rcd").writeBytes("content2".toByteArray())
        File(tempDir, "other.txt").writeBytes("ignored".toByteArray())

        // List documents
        val documents = fileServer.listDocuments()

        // Verify
        assertEquals(2, documents.size)
        assertTrue(documents.any { it.id == "doc1" })
        assertTrue(documents.any { it.id == "doc2" })
    }

    @Test
    fun `test list documents returns empty list for empty directory`() = runBlocking {
        // List documents in empty directory
        val documents = fileServer.listDocuments()

        // Verify
        assertEquals(0, documents.size)
    }

    @Test
    fun `test document metadata contains correct information`() = runBlocking {
        // Create a test document
        val documentId = "metadata-test"
        val content = "Test content for metadata".toByteArray()
        val file = File(tempDir, "$documentId.rcd")
        file.writeBytes(content)

        // List documents
        val documents = fileServer.listDocuments()

        // Verify metadata
        assertEquals(1, documents.size)
        val metadata = documents.first()
        assertEquals(documentId, metadata.id)
        assertEquals("$documentId.rcd", metadata.filename)
        assertEquals(content.size.toLong(), metadata.size)
        assertTrue(metadata.lastModified > 0)
    }

    @Test
    fun `test invalid document ID with path traversal is rejected`() {
        runBlocking {
            // Try to serve a document with path traversal
            assertFailsWith<IllegalArgumentException> {
                fileServer.serveDocument("../etc/passwd")
            }
        }
    }

    @Test
    fun `test invalid document ID with forward slash is rejected`() {
        runBlocking {
            // Try to serve a document with forward slash
            assertFailsWith<IllegalArgumentException> {
                fileServer.serveDocument("path/to/doc")
            }
        }
    }

    @Test
    fun `test invalid document ID with backslash is rejected`() {
        runBlocking {
            // Try to serve a document with backslash
            assertFailsWith<IllegalArgumentException> {
                fileServer.serveDocument("path\\to\\doc")
            }
        }
    }

    @Test
    fun `test concurrent document serving handles multiple requests`() = runBlocking {
        // Create test documents
        val documentIds = (1..10).map { "doc$it" }
        documentIds.forEach { id ->
            File(tempDir, "$id.rcd").writeBytes("Content for $id".toByteArray())
        }

        // Serve documents concurrently
        val results = documentIds.map { id ->
            async {
                fileServer.serveDocument(id)
            }
        }.awaitAll()

        // Verify all documents were served correctly
        assertEquals(10, results.size)
        results.forEachIndexed { index, bytes ->
            val expectedContent = "Content for doc${index + 1}"
            assertContentEquals(expectedContent.toByteArray(), bytes)
        }
    }

    @Test
    fun `test concurrent list operations complete successfully`() = runBlocking {
        // Create test documents
        repeat(5) { i ->
            File(tempDir, "doc$i.rcd").writeBytes("content$i".toByteArray())
        }

        // List documents concurrently
        val results = (1..20).map {
            async {
                fileServer.listDocuments()
            }
        }.awaitAll()

        // Verify all list operations returned the same results
        assertEquals(20, results.size)
        results.forEach { documents ->
            assertEquals(5, documents.size)
        }
    }
}
