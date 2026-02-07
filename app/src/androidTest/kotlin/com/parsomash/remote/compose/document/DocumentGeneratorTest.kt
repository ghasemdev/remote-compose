package com.parsomash.remote.compose.document

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class DocumentGeneratorJUnitTest {
    private lateinit var context: Context
    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var documentGenerator: DocumentGenerator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fileSystemManager = FileSystemManagerImpl(context)
        documentGenerator = DocumentGeneratorImpl(fileSystemManager)
    }

    @Test
    fun documentGeneratorShouldSaveDocumentBytes() = runTest {
        val documentId = "test_document"
        val testBytes = "test document content".toByteArray()

        // Save document bytes
        documentGenerator.saveDocumentBytes(documentId, testBytes)

        // Verify the document was saved
        val savedBytes = fileSystemManager.loadDocument(documentId)
        assertNotNull(savedBytes)
        assertArrayEquals(testBytes, savedBytes)
    }

    @Test
    fun fileSystemManagerShouldHandleDocumentLifecycle() = runTest {
        val documentId = "lifecycle_test"
        val testBytes = "lifecycle test content".toByteArray()

        // Initially document should not exist
        assertNull(fileSystemManager.loadDocument(documentId))

        // Save document
        fileSystemManager.saveDocument(documentId, testBytes)

        // Document should now exist
        val loadedBytes = fileSystemManager.loadDocument(documentId)
        assertNotNull(loadedBytes)
        assertArrayEquals(testBytes, loadedBytes)

        // Document should appear in list
        val documentList = fileSystemManager.listDocuments()
        assertTrue(documentList.contains(documentId))

        // Delete document
        fileSystemManager.deleteDocument(documentId)

        // Document should no longer exist
        assertNull(fileSystemManager.loadDocument(documentId))

        // Document should not appear in list
        val updatedList = fileSystemManager.listDocuments()
        assertFalse(updatedList.contains(documentId))
    }

    @Test
    fun fileSystemManagerShouldProvideCorrectDocumentsDirectory() {
        val documentsDir = fileSystemManager.getDocumentsDirectory()
        assertNotNull(documentsDir)
        assertTrue(documentsDir.contains("remote_compose_documents"))
    }
}
