package com.parsomash.remote.compose.document

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileSystemManagerTest {

    private lateinit var fileSystemManager: FileSystemManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fileSystemManager = FileSystemManagerImpl(context)
    }

    @After
    fun cleanup() = runTest {
        // Clean up test documents
        val documents = fileSystemManager.listDocuments()
        documents.forEach { documentId ->
            fileSystemManager.deleteDocument(documentId)
        }
    }

    @Test
    fun saveDocument_shouldCreateDocumentWithBasicMetadata() = runTest {
        val documentId = "test_basic_save"
        val testBytes = "test document content".toByteArray()

        // Save document
        fileSystemManager.saveDocument(documentId, testBytes)

        // Verify document exists
        assertTrue("Document should exist", fileSystemManager.documentExists(documentId))

        // Verify document content
        val loadedBytes = fileSystemManager.loadDocument(documentId)
        assertNotNull("Document should be loaded", loadedBytes)
        assertArrayEquals("Document content should match", testBytes, loadedBytes)

        // Verify metadata was created
        val metadata = fileSystemManager.loadDocumentMetadata(documentId)
        assertNotNull("Metadata should exist", metadata)
        assertEquals("Document ID should match", documentId, metadata!!.id)
        assertEquals("Title should match document ID", documentId, metadata.title)
        assertEquals("File size should match", testBytes.size.toLong(), metadata.fileSize)
        assertTrue("Created time should be set", metadata.createdAt > 0)
        assertTrue("Updated time should be set", metadata.updatedAt > 0)
        assertFalse("Checksum should not be empty", metadata.checksum.isEmpty())
    }

    @Test
    fun saveDocumentWithMetadata_shouldSaveCustomMetadata() = runTest {
        val documentId = "test_custom_metadata"
        val testBytes = "test document with custom metadata".toByteArray()
        val customMetadata = DocumentMetadata(
            id = documentId,
            version = "2.0.0",
            title = "Custom Test Document",
            description = "A test document with custom metadata",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tags = listOf("test", "custom", "metadata"),
            filePath = "", // Will be updated by implementation
            fileSize = 0L, // Will be updated by implementation
            checksum = "" // Will be updated by implementation
        )

        // Save document with custom metadata
        fileSystemManager.saveDocumentWithMetadata(documentId, testBytes, customMetadata)

        // Verify document exists
        assertTrue("Document should exist", fileSystemManager.documentExists(documentId))

        // Verify document content
        val loadedBytes = fileSystemManager.loadDocument(documentId)
        assertNotNull("Document should be loaded", loadedBytes)
        assertArrayEquals("Document content should match", testBytes, loadedBytes)

        // Verify custom metadata
        val loadedMetadata = fileSystemManager.loadDocumentMetadata(documentId)
        assertNotNull("Metadata should exist", loadedMetadata)
        assertEquals("Document ID should match", documentId, loadedMetadata!!.id)
        assertEquals("Version should match", "2.0.0", loadedMetadata.version)
        assertEquals("Title should match", "Custom Test Document", loadedMetadata.title)
        assertEquals(
            "Description should match",
            "A test document with custom metadata",
            loadedMetadata.description
        )
        assertEquals("Tags should match", listOf("test", "custom", "metadata"), loadedMetadata.tags)
        assertEquals(
            "File size should be updated",
            testBytes.size.toLong(),
            loadedMetadata.fileSize
        )
        assertFalse("File path should be set", loadedMetadata.filePath.isEmpty())
        assertFalse("Checksum should be calculated", loadedMetadata.checksum.isEmpty())
    }

    @Test
    fun loadDocumentMetadata_shouldReturnNullForNonExistentDocument() = runTest {
        val nonExistentId = "non_existent_document"

        val metadata = fileSystemManager.loadDocumentMetadata(nonExistentId)
        assertNull("Metadata should be null for non-existent document", metadata)
    }

    @Test
    fun deleteDocument_shouldRemoveDocumentAndMetadata() = runTest {
        val documentId = "test_delete"
        val testBytes = "document to be deleted".toByteArray()

        // Save document
        fileSystemManager.saveDocument(documentId, testBytes)
        assertTrue(
            "Document should exist before deletion",
            fileSystemManager.documentExists(documentId)
        )
        assertNotNull(
            "Metadata should exist before deletion",
            fileSystemManager.loadDocumentMetadata(documentId)
        )

        // Delete document
        fileSystemManager.deleteDocument(documentId)

        // Verify document and metadata are deleted
        assertFalse(
            "Document should not exist after deletion",
            fileSystemManager.documentExists(documentId)
        )
        assertNull(
            "Document content should be null after deletion",
            fileSystemManager.loadDocument(documentId)
        )
        assertNull(
            "Metadata should be null after deletion",
            fileSystemManager.loadDocumentMetadata(documentId)
        )
    }

    @Test
    fun listDocuments_shouldReturnAllDocumentIds() = runTest {
        val documentIds = listOf("doc1", "doc2", "doc3")
        val testBytes = "test content".toByteArray()

        // Save multiple documents
        documentIds.forEach { documentId ->
            fileSystemManager.saveDocument(documentId, testBytes)
        }

        // List documents
        val listedDocuments = fileSystemManager.listDocuments()

        // Verify all documents are listed
        documentIds.forEach { documentId ->
            assertTrue(
                "Document $documentId should be in the list",
                listedDocuments.contains(documentId)
            )
        }
    }

    @Test
    fun listDocumentsWithMetadata_shouldReturnAllMetadata() = runTest {
        val documentData = listOf(
            "doc1" to "content1",
            "doc2" to "content2",
            "doc3" to "content3"
        )

        // Save documents
        documentData.forEach { (documentId, content) ->
            fileSystemManager.saveDocument(documentId, content.toByteArray())
        }

        // List documents with metadata
        val metadataList = fileSystemManager.listDocumentsWithMetadata()

        // Verify metadata list
        assertEquals(
            "Should return metadata for all documents",
            documentData.size,
            metadataList.size
        )

        documentData.forEach { (documentId, _) ->
            val metadata = metadataList.find { it.id == documentId }
            assertNotNull("Metadata should exist for document $documentId", metadata)
            assertEquals("Document ID should match", documentId, metadata!!.id)
        }
    }

    @Test
    fun getDocumentSize_shouldReturnCorrectSize() = runTest {
        val documentId = "size_test"
        val testBytes = "test content for size calculation".toByteArray()

        // Save document
        fileSystemManager.saveDocument(documentId, testBytes)

        // Get document size
        val size = fileSystemManager.getDocumentSize(documentId)
        assertNotNull("Size should not be null", size)
        assertEquals("Size should match content length", testBytes.size.toLong(), size)
    }

    @Test
    fun getDocumentSize_shouldReturnNullForNonExistentDocument() = runTest {
        val nonExistentId = "non_existent_size_test"

        val size = fileSystemManager.getDocumentSize(nonExistentId)
        assertNull("Size should be null for non-existent document", size)
    }

    @Test
    fun documentExists_shouldReturnCorrectStatus() = runTest {
        val documentId = "existence_test"
        val testBytes = "test content".toByteArray()

        // Initially should not exist
        assertFalse(
            "Document should not exist initially",
            fileSystemManager.documentExists(documentId)
        )

        // Save document
        fileSystemManager.saveDocument(documentId, testBytes)
        assertTrue(
            "Document should exist after saving",
            fileSystemManager.documentExists(documentId)
        )

        // Delete document
        fileSystemManager.deleteDocument(documentId)
        assertFalse(
            "Document should not exist after deletion",
            fileSystemManager.documentExists(documentId)
        )
    }

    @Test
    fun getDocumentsDirectory_shouldReturnValidPath() {
        val documentsDir = fileSystemManager.getDocumentsDirectory()

        assertNotNull("Documents directory should not be null", documentsDir)
        assertTrue(
            "Documents directory should contain expected path",
            documentsDir.contains("remote_compose_documents")
        )
    }

    @Test
    fun checksumCalculation_shouldBeConsistent() = runTest {
        val documentId1 = "checksum_test1"
        val documentId2 = "checksum_test2"
        val sameContent = "identical content".toByteArray()
        val differentContent = "different content".toByteArray()

        // Save documents with same content
        fileSystemManager.saveDocument(documentId1, sameContent)
        fileSystemManager.saveDocument(documentId2, sameContent)

        val metadata1 = fileSystemManager.loadDocumentMetadata(documentId1)
        val metadata2 = fileSystemManager.loadDocumentMetadata(documentId2)

        assertNotNull("Metadata1 should exist", metadata1)
        assertNotNull("Metadata2 should exist", metadata2)
        assertEquals(
            "Checksums should be identical for same content",
            metadata1!!.checksum, metadata2!!.checksum
        )

        // Save document with different content
        val documentId3 = "checksum_test3"
        fileSystemManager.saveDocument(documentId3, differentContent)
        val metadata3 = fileSystemManager.loadDocumentMetadata(documentId3)

        assertNotNull("Metadata3 should exist", metadata3)
        assertNotEquals(
            "Checksums should be different for different content",
            metadata1.checksum, metadata3!!.checksum
        )
    }

    @Test
    fun metadataUpdate_shouldUpdateTimestamp() = runTest {
        val documentId = "timestamp_test"
        val originalContent = "original content".toByteArray()
        val updatedContent = "updated content".toByteArray()

        // Save original document
        fileSystemManager.saveDocument(documentId, originalContent)
        val originalMetadata = fileSystemManager.loadDocumentMetadata(documentId)
        assertNotNull("Original metadata should exist", originalMetadata)

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10)

        // Update document with new metadata
        val updatedMetadata = originalMetadata!!.copy(
            title = "Updated Title",
            description = "Updated description"
        )
        fileSystemManager.saveDocumentWithMetadata(documentId, updatedContent, updatedMetadata)

        // Verify updated metadata
        val finalMetadata = fileSystemManager.loadDocumentMetadata(documentId)
        assertNotNull("Final metadata should exist", finalMetadata)
        assertEquals("Title should be updated", "Updated Title", finalMetadata!!.title)
        assertEquals(
            "Description should be updated",
            "Updated description",
            finalMetadata.description
        )
        assertTrue(
            "Updated timestamp should be later",
            finalMetadata.updatedAt > originalMetadata.updatedAt
        )
        assertEquals(
            "File size should reflect new content",
            updatedContent.size.toLong(), finalMetadata.fileSize
        )
    }
}
