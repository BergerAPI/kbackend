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
open class Response(
    val status: HttpResponseStatus,
    val body: String,
    val type: ResponseType,
    val headers: Map<String, String>
)

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
 * This is a route that handles a request onto a specific path.
 */
data class Route(
    val method: HttpMethod,
    val path: String,
    val handler: (Request) -> Response
)

/**
 * A listener is a class that can have multiple routes.
 */
abstract class Controller

/**
 * Generating a JsonResponse (the fast way)
 */
fun json(
    value: Any,
    status: HttpResponseStatus = HttpResponseStatus.OK,
    headers: Map<String, String> = emptyMap()
): Response =
    Response(status, Gson().toJson(value), ResponseType.JSON, headers)

/**
 * Generating a plain text response (the fast way)
 */
fun plain(
    value: String,
    status: HttpResponseStatus = HttpResponseStatus.OK,
    headers: Map<String, String> = emptyMap()
): Response =
    Response(status, value, ResponseType.TEXT, headers)

/**
 * Redirecting the user to another page.
 */
fun redirect(
    path: String,
    status: HttpResponseStatus = HttpResponseStatus.FOUND,
    headers: Map<String, String> = emptyMap()
): Response =
    Response(status, "", ResponseType.TEXT, headers + mapOf("Location" to path))

/**
 * Controllers can have a predefined prefix
 */
annotation class Path(val value: String)

/**
 * This can be put on to a parameter in a function to make it required.
 */
annotation class Query(val value: String)

/**
 * Listener function annotations to handle routes
 */
annotation class GET(val path: String)
annotation class POST(val path: String)
annotation class PUT(val path: String)
annotation class DELETE(val path: String)
annotation class HEAD(val path: String)
annotation class OPTIONS(val path: String)

class RouteHandler(private val manager: BackendManager) {

    /**
     * All our registered routes
     */
    private var routes: ArrayList<Route> = arrayListOf()

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

    /**
     * This runs the code on the server and returns a response.
     */
    fun getResponse(req: Request): Response {
        val route = routes.find { it.method == req.method && it.path == req.path }

        // Running the middleware
        val middlewareResponse = this.manager.middleware.preRequest(req)

        if (middlewareResponse.failed)
            return json(middlewareResponse, middlewareResponse.status)

        return route?.handler?.invoke(req) ?: Response(HttpResponseStatus.NOT_FOUND, "Not found", ResponseType.TEXT, emptyMap())
    }

    /**
     * Register a route.
     */
    fun lazyRoute(path: String, handler: (Request) -> Response) {
        routes.add(Route(HttpMethod.GET, path, handler))
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
            val (path, method) = it.annotations.first { annotation ->
                evaluateAnnotation(annotation) != null
            }.let(::evaluateAnnotation)!!

            // Checking the return type
            if (it.returnType.name != "de.berger.Response")
                throw IllegalArgumentException("Route handler must return a Response")

            // Getting all parameters with the Query annotation
            val queries = it.parameters.filter { param ->
                param.annotations.any { annotation ->
                    annotation is Query
                }
            }.map { param ->
                param.annotations.find { annotation ->
                    annotation is Query
                }!!.let { annotation ->
                    Pair((annotation as Query).value, param.type)
                }
            }

            routes.add(Route(method, (prefix + path)) { req ->
                // if we have queries, we need to add them to the request
                if (queries.isNotEmpty()) {
                    val queryMap = queries.associate { mapIt -> mapIt.first to req.queries[mapIt.first] }
                    val paramArray = arrayListOf<Any>()

                    paramArray.add(req)

                    val failedQuery = arrayListOf<String>()

                    // Every query is a function parameter, so we need to map them to an Object[]
                    paramArray.addAll(queryMap.map { mapIt ->
                        // Checking if this is an integer
                        val notNullValue = mapIt.value

                        if (notNullValue != null) {
                            if (notNullValue.toIntOrNull() != null)
                                return@map notNullValue.toInt()

                            if (notNullValue.toDoubleOrNull() != null)
                                return@map notNullValue.toDouble()

                            if (notNullValue.toBoolean())
                                return@map notNullValue.toBoolean()

                            return@map notNullValue
                        }

                        failedQuery.add(mapIt.key)
                    })

                    if (failedQuery.size != 0)
                        Response(
                            HttpResponseStatus.BAD_REQUEST,
                            "Missing query parameters: ${failedQuery.joinToString(", ")}",
                            ResponseType.TEXT,
                            emptyMap()
                        )
                    else {
                        try {
                            it.invoke(controller, *paramArray.toTypedArray()) as Response
                        } catch (e: Exception) {
                            Response(
                                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                "Invocation Error: ${e.message}",
                                ResponseType.TEXT,
                                emptyMap()
                            )
                        }
                    }
                } else it.invoke(controller, req) as Response
            })
        }
    }

}