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
        var failed = false
        var response = ""
        var status = HttpResponseStatus.OK

        for (middleware in middlewares) {
            val middlewareResponse = middleware.preRequest(request)

            if (middlewareResponse.failed) {
                failed = true
                response = middlewareResponse.response
                status = middlewareResponse.status
                break
            }
        }

        return MiddlewareResponse(failed, response, status)
    }
}