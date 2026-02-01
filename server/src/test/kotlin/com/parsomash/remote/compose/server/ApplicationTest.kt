package com.parsomash.remote.compose.server

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class ApplicationTest : StringSpec({
    "server should respond to root endpoint" {
        testApplication {
            application {
                module()
            }
            client.get("/").apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "Remote Compose Server is running!"
            }
        }
    }
})