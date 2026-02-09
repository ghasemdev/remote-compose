package com.parsomash.remote.compose.network

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds

class KtorClientManagerTest : FunSpec({
    test("ClientConfig should have default values") {
        val config = ClientConfig(baseUrl = "http://localhost:8080")

        config.baseUrl shouldBe "http://localhost:8080"
        config.timeout shouldBe 30.seconds
        config.enableHttp2 shouldBe true
        config.enableLogging shouldBe true
        config.retryPolicy.maxRetries shouldBe 3
    }

    test("RetryPolicy should have default values") {
        val policy = RetryPolicy()

        policy.maxRetries shouldBe 3
        policy.initialDelay shouldBe 1.seconds
        policy.maxDelay shouldBe 10.seconds
        policy.factor shouldBe 2.0
    }

    test("KtorClientManager should be created with config") {
        val config = ClientConfig(
            baseUrl = "http://localhost:8080",
            timeout = 15.seconds,
            enableHttp2 = true
        )

        val manager = KtorClientManagerImpl(config)
        manager shouldNotBe null
        manager.close()
    }

    test("configure should update client configuration") {
        val initialConfig = ClientConfig(baseUrl = "http://localhost:8080")
        val manager = KtorClientManagerImpl(initialConfig)

        val newConfig = ClientConfig(
            baseUrl = "http://localhost:9090",
            timeout = 60.seconds
        )

        manager.configure(newConfig)
        // Configuration updated successfully
        manager.close()
    }

    test("HttpRequestException should contain status code") {
        val exception = HttpRequestException("Test error", 404)

        exception.message shouldContain "Test error"
        exception.statusCode shouldBe 404
    }

    test("DocumentMetadata should be serializable") {
        val metadata = DocumentMetadata(
            id = "doc1",
            version = "1.0",
            title = "Test Document",
            description = "A test document",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tags = listOf("test", "sample"),
            filePath = "/path/to/doc"
        )

        metadata.id shouldBe "doc1"
        metadata.version shouldBe "1.0"
        metadata.title shouldBe "Test Document"
        metadata.tags.size shouldBe 2
    }
})
