package de.berger

import io.netty.handler.codec.http.HttpResponseStatus

/**
 * This is what a middleware returns. If failed is true, the request will be aborted and will return the response.
 */
data class MiddlewareResponse(val failed: Boolean, val response: String, val status: HttpResponseStatus)

/**
 * This is the interface for a middleware.
 */
interface Middleware {
    fun preRequest(request: Request): MiddlewareResponse
}

/**
 * An annotation for a middleware that should only protect one endpoint.
 */
annotation class Protect(val path: String)

/**
 * This handler takes care about the all middlewares.
 */
class MiddlewareHandler {
    private val middlewares: MutableList<Middleware> = mutableListOf()

    /**
     * Adds a middleware to the list.
     */
    fun addMiddleware(middleware: Middleware) {
        middlewares.add(middleware)
    }

    /**
     * This method will be called before the request is executed.
     */
    fun preRequest(request: Request): MiddlewareResponse {
        for (middleware in middlewares) {
            val middlewareResponse = middleware.preRequest(request)

            if (middlewareResponse.failed)
                return MiddlewareResponse(true, middlewareResponse.response, middlewareResponse.status)
        }

        return MiddlewareResponse(false, "", HttpResponseStatus.OK)
    }
}