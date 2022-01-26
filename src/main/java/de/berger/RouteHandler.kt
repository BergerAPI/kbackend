package de.berger

import com.google.gson.Gson
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus

/**
 * A response can return just plain text, or a JSON object.
 */
enum class ResponseType(val contentType: String) {
    JSON("application/json"),
    TEXT("text/plain")
}

/**
 * This is what we return to the client.
 */
open class Response(val status: HttpResponseStatus, val body: String, val type: ResponseType)

/**
 * This is what we get from the client.
 */
data class Request(
    val method: HttpMethod,
    val path: String,
    val body: String,
    val headers: Map<String, String>,
    val queries: Map<String, String>
)

/**
 * A listener is a class that can have multiple routes.
 */
abstract class Controller

/**
 * Generating a JsonResponse (the fast way)
 */
fun json(value: Any, status: HttpResponseStatus = HttpResponseStatus.OK): Response =
    Response(status, Gson().toJson(value), ResponseType.JSON)

/**
 * Generating a plain text response (the fast way)
 */
fun plain(value: String, status: HttpResponseStatus = HttpResponseStatus.OK): Response =
    Response(status, value, ResponseType.TEXT)

/**
 * Controllers can have a predefined prefix
 */
annotation class Path(val value: String)

/**
 * Listener function annotations to handle routes
 */
annotation class GET(val path: String)
annotation class POST(val path: String)
annotation class PUT(val path: String)
annotation class DELETE(val path: String)
annotation class HEAD(val path: String)
annotation class OPTIONS(val path: String)

class RouteHandler {

    /**
     * All our registered routes
     */
    private var routes: Map<String, (Request) -> Response> = mapOf()

    /**
     * Checking if an annotation is one of ours
     */
    private fun evaluateAnnotation(annotation: Annotation): Pair<String, HttpMethod>? {
        return when (annotation) {
            is GET -> Pair(annotation.path, HttpMethod.GET)
            is POST -> Pair(annotation.path, HttpMethod.POST)
            is PUT -> Pair(annotation.path, HttpMethod.PUT)
            is DELETE -> Pair(annotation.path, HttpMethod.DELETE)
            is HEAD -> Pair(annotation.path, HttpMethod.HEAD)
            is OPTIONS -> Pair(annotation.path, HttpMethod.OPTIONS)
            else -> null
        }
    }

    /**
     * Getting all queries from an uri
     */
    private fun decodeUri(uri: String): Pair<String, Map<String, String>> {
        if (uri.contains("?")) {
            val split = uri.split("?")

            return Pair(split[0], split[1].split("&").map { it.split("=") }.associate { it[0] to it[1] })
        }

        return Pair(uri, mapOf())
    }

    /**
     * Converting a Netty FullHttpRequest to our Request.
     */
    fun transformRequest(req: FullHttpRequest): Request {
        val body = req.content().toString(Charsets.UTF_8)
        val headers = req.headers().entries().associate { it.key.toString() to it.value.toString() }
        val uri = req.uri().toString()

        // Decoding the path to query parameters
        val (path, queries) = decodeUri(uri)

        return Request(req.method()!!, path, body, headers, queries)
    }

    fun getResponse(req: Request): Response {
        val route = routes[req.path]
        return route?.invoke(req) ?: Response(HttpResponseStatus.NOT_FOUND, "Not found", ResponseType.TEXT)
    }

    /**
     * Register a route.
     */
    fun lazyRoute(path: String, handler: (Request) -> Response) {
        routes = routes + (path to handler)
    }

    /**
     * Registering a listener with multiple routes.
     */
    fun initializeController(controller: Controller) {
        // Checking if the controller has a prefix
        val prefix = controller.javaClass.annotations.find { it is Path }?.let {
            (it as Path).value
        } ?: ""

        controller.javaClass.declaredMethods.filter {
            it.annotations.any { annotation ->
                evaluateAnnotation(annotation) != null
            }
        }.forEach {
            val (path, _) = it.annotations.first { annotation ->
                evaluateAnnotation(annotation) != null
            }.let(::evaluateAnnotation)!!

            // Checking the return type
            if (it.returnType.name != "de.berger.Response")
                throw IllegalArgumentException("Route handler must return a Response")

            routes = routes + ((prefix + path) to { req ->
                it.invoke(controller, req) as Response
            })
        }
    }

}