package com.parsomash.remote.compose.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the Ktor client
 */
data class ClientConfig(
    val baseUrl: String,
    val timeout: Duration = 30.seconds,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val enableHttp2: Boolean = true,
    val enableLogging: Boolean = true
)

/**
 * Retry policy configuration with exponential backoff
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 10.seconds,
    val factor: Double = 2.0
)

/**
 * Metadata for a document
 */
@Serializable
data class DocumentMetadata(
    val id: String,
    val version: String,
    val title: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val filePath: String
)

/**
 * Interface for managing Ktor client operations
 */
interface KtorClientManager {
    /**
     * Fetch a document by its ID
     * @param documentId The ID of the document to fetch
     * @return The document bytes
     * @throws Exception if the document cannot be fetched after all retries
     */
    suspend fun fetchDocument(documentId: String): ByteArray

    /**
     * Fetch the list of available documents
     * @return List of document metadata
     * @throws Exception if the list cannot be fetched after all retries
     */
    suspend fun fetchDocumentList(): List<DocumentMetadata>

    /**
     * Configure the client with new settings
     * @param config The new configuration
     */
    fun configure(config: ClientConfig)

    /**
     * Close the client and release resources
     */
    fun close()
}

/**
 * Implementation of KtorClientManager with HTTP/2 support, retry logic, and timeout handling
 */
class KtorClientManagerImpl(
    private var config: ClientConfig
) : KtorClientManager {

    private val tag = "KtorClientManager"

    private var client: HttpClient = createClient()

    private fun createClient(): HttpClient {
        return HttpClient(OkHttp) {
            // Timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = config.timeout.inWholeMilliseconds
                connectTimeoutMillis = config.timeout.inWholeMilliseconds
                socketTimeoutMillis = config.timeout.inWholeMilliseconds
            }

            // Retry plugin with exponential backoff
            install(HttpRequestRetry) {
                maxRetries = config.retryPolicy.maxRetries
                retryOnServerErrors(maxRetries = config.retryPolicy.maxRetries)
                retryOnException(maxRetries = config.retryPolicy.maxRetries, retryOnTimeout = true)

                exponentialDelay(
                    base = config.retryPolicy.factor,
                    maxDelayMs = config.retryPolicy.maxDelay.inWholeMilliseconds
                )

                modifyRequest { request ->
                    println("$tag Retrying request: ${request.url}")
                }
            }

            // Content negotiation for JSON
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            // Logging
            if (config.enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("$tag $message")
                        }
                    }
                    level = LogLevel.INFO
                }
            }

            // Default request configuration
            defaultRequest {
                url(config.baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }

    override suspend fun fetchDocument(documentId: String): ByteArray {
        val response = client.get("documents/$documentId")

        if (response.status.isSuccess()) {
            return response.readRawBytes()
        } else {
            throw HttpRequestException(
                "Failed to fetch document: ${response.status}",
                response.status.value
            )
        }
    }

    override suspend fun fetchDocumentList(): List<DocumentMetadata> {
        val response = client.get("documents")

        if (response.status.isSuccess()) {
            return response.body<List<DocumentMetadata>>()
        } else {
            throw HttpRequestException(
                "Failed to fetch document list: ${response.status}",
                response.status.value
            )
        }
    }

    override fun configure(config: ClientConfig) {
        this.config = config
        client.close()
        client = createClient()
        println("$tag Client reconfigured with new settings")
    }

    override fun close() {
        client.close()
        println("$tag Client closed")
    }
}

/**
 * Custom exception for HTTP request failures
 */
class HttpRequestException(
    message: String,
    val statusCode: Int
) : Exception(message)
