package com.parsomash.remote.compose.document

import com.parsomash.remote.compose.network.HttpRequestException
import com.parsomash.remote.compose.network.KtorClientManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class DocumentRenderingServiceTest : FunSpec({

    test("fetchDocument should handle network errors properly") {
        // Arrange
        val mockKtorClient = mockk<KtorClientManager>()
        val mockPlayerService = mockk<DocumentPlayerService>()
        val service = DocumentRenderingServiceImpl(mockKtorClient, mockPlayerService)

        val networkException = HttpRequestException("Server error", 500)
        coEvery { mockKtorClient.fetchDocument("test-doc") } throws networkException

        // Act & Assert
        val exception = shouldThrow<DocumentRenderingError.NetworkError> {
            service.fetchDocument("test-doc")
        }

        exception.statusCode shouldBe 500
        exception.cause shouldBe networkException
        exception.message shouldContain "HTTP 500"
    }

    test("fetchDocument should handle generic network errors") {
        // Arrange
        val mockKtorClient = mockk<KtorClientManager>()
        val mockPlayerService = mockk<DocumentPlayerService>()
        val service = DocumentRenderingServiceImpl(mockKtorClient, mockPlayerService)

        val genericException = RuntimeException("Connection failed")
        coEvery { mockKtorClient.fetchDocument("test-doc") } throws genericException

        // Act & Assert
        val exception = shouldThrow<DocumentRenderingError.NetworkError> {
            service.fetchDocument("test-doc")
        }

        exception.statusCode shouldBe null
        exception.cause shouldBe genericException
    }

    test("DocumentRenderingError types should have proper inheritance") {
        val networkError = DocumentRenderingError.NetworkError(
            statusCode = 404,
            cause = RuntimeException("Not found")
        )
        val deserializationError = DocumentRenderingError.DeserializationError(
            documentId = "test",
            cause = RuntimeException("Invalid format")
        )
        val renderingError = DocumentRenderingError.RenderingError(
            documentId = "test",
            cause = RuntimeException("Render failed")
        )
        val operationError = DocumentRenderingError.OperationError(
            operation = "fetch",
            cause = RuntimeException("Operation failed")
        )

        networkError.shouldBeInstanceOf<DocumentRenderingError>()
        deserializationError.shouldBeInstanceOf<DocumentRenderingError>()
        renderingError.shouldBeInstanceOf<DocumentRenderingError>()
        operationError.shouldBeInstanceOf<DocumentRenderingError>()

        networkError.message shouldContain "HTTP 404"
        deserializationError.message shouldContain "test"
        renderingError.message shouldContain "test"
        operationError.message shouldContain "fetch"
    }

    test("createDocumentRenderingService factory should create proper instance") {
        val mockKtorClient = mockk<KtorClientManager>()
        val mockPlayerService = mockk<DocumentPlayerService>()

        val service = createDocumentRenderingService(mockKtorClient, mockPlayerService)

        service.shouldBeInstanceOf<DocumentRenderingServiceImpl>()
    }

    test("DocumentRenderingService integration components should be properly connected") {
        // Arrange
        val mockKtorClient = mockk<KtorClientManager>()
        val mockPlayerService = mockk<DocumentPlayerService>()
        val service = DocumentRenderingServiceImpl(mockKtorClient, mockPlayerService)

        // This test verifies that the service properly integrates the components
        // without testing the complex deserialization logic
        service.shouldBeInstanceOf<DocumentRenderingService>()

        // Verify that network errors are properly wrapped
        val networkException = HttpRequestException("Test error", 404)
        coEvery { mockKtorClient.fetchDocument("test-doc") } throws networkException

        val exception = shouldThrow<DocumentRenderingError.NetworkError> {
            service.fetchDocument("test-doc")
        }

        // Verify the integration properly calls the network layer
        coVerify { mockKtorClient.fetchDocument("test-doc") }
        exception.statusCode shouldBe 404
    }
})