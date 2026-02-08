package com.parsomash.remote.compose.server

import com.parsomash.remote.compose.server.files.DocumentNotFoundException
import com.parsomash.remote.compose.server.files.KtorFileServer
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
}
