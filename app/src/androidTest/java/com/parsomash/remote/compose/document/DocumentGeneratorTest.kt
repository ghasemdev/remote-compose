package com.parsomash.remote.compose.document

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentGeneratorTest {

    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var documentGenerator: DocumentGenerator

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fileSystemManager = FileSystemManagerImpl(context)
        documentGenerator = DocumentGeneratorImpl(fileSystemManager)
    }

    @Test
    fun documentGenerator_shouldSaveDocumentBytes() = runTest {
        val documentId = "test_document"
        val testBytes = "test document content".toByteArray()

        // Save document bytes
        documentGenerator.saveDocumentBytes(documentId, testBytes)

        // Verify the document was saved
        val savedBytes = fileSystemManager.loadDocument(documentId)
        assertNotNull("Document should be saved", savedBytes)
        assertArrayEquals("Document content should match", testBytes, savedBytes)
    }

    @Test
    fun fileSystemManager_shouldHandleDocumentLifecycle() = runTest {
        val documentId = "lifecycle_test"
        val testBytes = "lifecycle test content".toByteArray()

        // Initially document should not exist
        assertNull(
            "Document should not exist initially",
            fileSystemManager.loadDocument(documentId)
        )

        // Save document
        fileSystemManager.saveDocument(documentId, testBytes)

        // Document should now exist
        val loadedBytes = fileSystemManager.loadDocument(documentId)
        assertNotNull("Document should exist after saving", loadedBytes)
        assertArrayEquals("Document content should match", testBytes, loadedBytes)

        // Document should appear in list
        val documentList = fileSystemManager.listDocuments()
        assertTrue("Document should appear in list", documentList.contains(documentId))

        // Delete document
        fileSystemManager.deleteDocument(documentId)

        // Document should no longer exist
        assertNull(
            "Document should not exist after deletion",
            fileSystemManager.loadDocument(documentId)
        )

        // Document should not appear in list
        val updatedList = fileSystemManager.listDocuments()
        assertFalse(
            "Document should not appear in list after deletion",
            updatedList.contains(documentId)
        )
    }

    @Test
    fun fileSystemManager_shouldProvideCorrectDocumentsDirectory() {
        val documentsDir = fileSystemManager.getDocumentsDirectory()
        assertNotNull("Documents directory should not be null", documentsDir)
        assertTrue(
            "Documents directory should contain expected path",
            documentsDir.contains("remote_compose_documents")
        )
    }
}