package de.berger

import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL

class MiddleWareTest : Middleware {
    override fun preRequest(request: Request): MiddlewareResponse {
        return MiddlewareResponse(true, "Failed.", HttpResponseStatus.OK)
    }
}

class ControllerTest : Controller() {

    @GET("/test")
    @Protect("de.berger.MiddleWareTest")
    fun testEndPoint(request: Request): Response {
        return plain("Hello World")
    }

    @GET("/test2")
    fun testEndPoint2(request: Request): Response {
        return plain("Hello World")
    }

}

internal class BackendManagerTest {
    private fun sendGet(uri: String): ArrayList<String> {
        val url = URL(uri)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connect()

        val response = connection.inputStream.bufferedReader().readText()
        return ArrayList(response.split("\n"))
    }

    @Test
    fun checkIfBackendManagerStartsWithLazyRoute() {
        Thread {
            val app = RestApp()

            app.routing.lazyRoute("/") {
                plain("test")
            }

            app.routing.initializeController(ControllerTest())

            app.run(3000)
        }.start()

        Thread.sleep(1000)

        var response = sendGet("http://localhost:3000/")

        println("Response: $response")

        assertEquals("test", response[0])

        response = sendGet("http://localhost:3000/test")

        println("Response: $response")

        assertEquals("{\"failed\":true,\"response\":\"Failed.\",\"status\":{\"code\":200,\"codeAsText\":{\"value\":[50,48,48],\"offset\":0,\"length\":3,\"hash\":0},\"codeClass\":\"SUCCESS\",\"reasonPhrase\":\"OK\",\"bytes\":[50,48,48,32,79,75]}}", response[0])

        response = sendGet("http://localhost:3000/test2")

        println("Response: $response")

        assertEquals("Hello World", response[0])
    }
}