package de.berger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL

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

            app.run(3000)
        }.start()

        Thread.sleep(1000)

        val response = sendGet("http://localhost:3000/")

        println("Response: $response")

        assertEquals("test", response[0])
    }
}