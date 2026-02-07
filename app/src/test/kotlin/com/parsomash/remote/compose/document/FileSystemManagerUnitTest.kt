package com.parsomash.remote.compose.document

import android.content.Context
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files

class FileSystemManagerUnitTest : DescribeSpec({

    describe("FileSystemManager") {
        lateinit var tempDir: File
        lateinit var context: Context
        lateinit var fileSystemManager: FileSystemManager

        beforeEach {
            // Create temporary directory for testing
            tempDir = Files.createTempDirectory("test_remote_compose").toFile()

            // Mock Android Context
            context = mockk<Context>()
            every { context.filesDir } returns tempDir

            fileSystemManager = FileSystemManagerImpl(context)
        }

        afterEach {
            // Clean up temporary directory
            tempDir.deleteRecursively()
        }

        describe("basic document operations") {
            it("should save and load document") {
                runTest {
                    val documentId = "test_document"
                    val testBytes = "test content".toByteArray()

                    fileSystemManager.saveDocument(documentId, testBytes)

                    val loadedBytes = fileSystemManager.loadDocument(documentId)
                    loadedBytes.shouldNotBeNull()
                    loadedBytes shouldBe testBytes
                }
            }

            it("should return null for non-existent document") {
                runTest {
                    val loadedBytes = fileSystemManager.loadDocument("non_existent")
                    loadedBytes.shouldBeNull()
                }
            }

            it("should delete document and metadata") {
                runTest {
                    val documentId = "delete_test"
                    val testBytes = "content to delete".toByteArray()

                    fileSystemManager.saveDocument(documentId, testBytes)
                    fileSystemManager.documentExists(documentId) shouldBe true

                    fileSystemManager.deleteDocument(documentId)
                    fileSystemManager.documentExists(documentId) shouldBe false
                    fileSystemManager.loadDocument(documentId).shouldBeNull()
                    fileSystemManager.loadDocumentMetadata(documentId).shouldBeNull()
                }
            }
        }

        describe("metadata operations") {
            it("should create basic metadata when saving document") {
                runTest {
                    val documentId = "metadata_test"
                    val testBytes = "test content with metadata".toByteArray()

                    fileSystemManager.saveDocument(documentId, testBytes)

                    val metadata = fileSystemManager.loadDocumentMetadata(documentId)
                    metadata.shouldNotBeNull()
                    metadata.id shouldBe documentId
                    metadata.title shouldBe documentId
                    metadata.fileSize shouldBe testBytes.size.toLong()
                    metadata.createdAt shouldNotBe 0L
                    metadata.updatedAt shouldNotBe 0L
                    metadata.checksum shouldNotBe ""
                }
            }

            it("should save custom metadata") {
                runTest {
                    val documentId = "custom_metadata_test"
                    val testBytes = "custom metadata content".toByteArray()
                    val customMetadata = DocumentMetadata(
                        id = documentId,
                        version = "2.0.0",
                        title = "Custom Title",
                        description = "Custom description",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        tags = listOf("custom", "test"),
                        filePath = "",
                        fileSize = 0L,
                        checksum = ""
                    )

                    fileSystemManager.saveDocumentWithMetadata(
                        documentId,
                        testBytes,
                        customMetadata
                    )

                    val loadedMetadata = fileSystemManager.loadDocumentMetadata(documentId)
                    loadedMetadata.shouldNotBeNull()
                    loadedMetadata.version shouldBe "2.0.0"
                    loadedMetadata.title shouldBe "Custom Title"
                    loadedMetadata.description shouldBe "Custom description"
                    loadedMetadata.tags shouldBe listOf("custom", "test")
                    loadedMetadata.fileSize shouldBe testBytes.size.toLong()
                    loadedMetadata.filePath shouldContain documentId
                    loadedMetadata.checksum shouldNotBe ""
                }
            }

            it("should return null metadata for non-existent document") {
                runTest {
                    val metadata = fileSystemManager.loadDocumentMetadata("non_existent")
                    metadata.shouldBeNull()
                }
            }
        }

        describe("document listing") {
            it("should list all document IDs") {
                runTest {
                    val documentIds = listOf("doc1", "doc2", "doc3")
                    val testBytes = "test content".toByteArray()

                    documentIds.forEach { documentId ->
                        fileSystemManager.saveDocument(documentId, testBytes)
                    }

                    val listedDocuments = fileSystemManager.listDocuments()
                    listedDocuments shouldHaveSize documentIds.size
                    documentIds.forEach { documentId ->
                        listedDocuments shouldContain documentId
                    }
                }
            }

            it("should list documents with metadata") {
                runTest {
                    val documentData = mapOf(
                        "doc1" to "content1",
                        "doc2" to "content2"
                    )

                    documentData.forEach { (documentId, content) ->
                        fileSystemManager.saveDocument(documentId, content.toByteArray())
                    }

                    val metadataList = fileSystemManager.listDocumentsWithMetadata()
                    metadataList shouldHaveSize documentData.size

                    documentData.keys.forEach { documentId ->
                        val metadata = metadataList.find { it.id == documentId }
                        metadata.shouldNotBeNull()
                        metadata.id shouldBe documentId
                    }
                }
            }
        }

        describe("utility operations") {
            it("should return correct document size") {
                runTest {
                    val documentId = "size_test"
                    val testBytes = "content for size test".toByteArray()

                    fileSystemManager.saveDocument(documentId, testBytes)

                    val size = fileSystemManager.getDocumentSize(documentId)
                    size.shouldNotBeNull()
                    size shouldBe testBytes.size.toLong()
                }
            }

            it("should return null size for non-existent document") {
                runTest {
                    val size = fileSystemManager.getDocumentSize("non_existent")
                    size.shouldBeNull()
                }
            }

            it("should check document existence correctly") {
                runTest {
                    val documentId = "existence_test"
                    val testBytes = "existence test content".toByteArray()

                    fileSystemManager.documentExists(documentId) shouldBe false

                    fileSystemManager.saveDocument(documentId, testBytes)
                    fileSystemManager.documentExists(documentId) shouldBe true

                    fileSystemManager.deleteDocument(documentId)
                    fileSystemManager.documentExists(documentId) shouldBe false
                }
            }

            it("should provide valid documents directory") {
                val documentsDir = fileSystemManager.getDocumentsDirectory()
                documentsDir.shouldNotBeNull()
                documentsDir shouldContain "remote_compose_documents"
            }
        }

        describe("checksum calculation") {
            it("should generate consistent checksums for same content") {
                runTest {
                    val content = "identical content".toByteArray()

                    fileSystemManager.saveDocument("doc1", content)
                    fileSystemManager.saveDocument("doc2", content)

                    val metadata1 = fileSystemManager.loadDocumentMetadata("doc1")
                    val metadata2 = fileSystemManager.loadDocumentMetadata("doc2")

                    metadata1.shouldNotBeNull()
                    metadata2.shouldNotBeNull()
                    metadata1.checksum shouldBe metadata2.checksum
                }
            }

            it("should generate different checksums for different content") {
                runTest {
                    fileSystemManager.saveDocument("doc1", "content1".toByteArray())
                    fileSystemManager.saveDocument("doc2", "content2".toByteArray())

                    val metadata1 = fileSystemManager.loadDocumentMetadata("doc1")
                    val metadata2 = fileSystemManager.loadDocumentMetadata("doc2")

                    metadata1.shouldNotBeNull()
                    metadata2.shouldNotBeNull()
                    metadata1.checksum shouldNotBe metadata2.checksum
                }
            }
        }
    }
})