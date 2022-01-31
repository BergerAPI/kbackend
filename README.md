# kbackend
A simple backend library for creating backends in Kotlin/Java

# Maven Setup

Add this as dependency:
```xml
<dependency>
    <groupId>de.berger</groupId>
    <artifactId>kbackend</artifactId>
    <version>1.0.2</version>
</dependency>
```

# Usage

## Lazy Routes

A lazy route is basically a route, which is required to **only** listen to GET request and makes it easier to debug things.

```kotlin
backend.routing.lazyRoute("/") {
    return plain("Hello World!")
}
```

## Controller

Controller exists, so you can easily create multiple routes as functions and use annotations for information passed by the user like the queries, or the body.

```kotlin

class Controller : Controller() {
    
    @GET("/path") // There are also @POST, @PUT, @DELETE, @PATCH, @HEAD, @OPTIONS
    @Protect("de.berger.MiddleWareTest") // This applies a middleware, which is executed before the route is executed and decides if the route is executed or not.
    fun myEndpoint(request: Request, 
                   @Body(MyBodyObject::class) myBody: MyBodyObject, // This is the body of the request, which is parsed by the body parser. It accepts your custom class.
                   @Query("intQuery") testQuery: Int, // The type of the query is decided by the type of the parameter, so this has the type int
                   @Query("stringQuery") testQuery: String // and this has the type string
    ): Response {
        return plain("Hello World!") // We also have json(myObject, status?) and redirect(route, status?) 
    }
    
}
```

## Middleware

Middlewares are always executed before the route is executed and decide if the route is executed or not.

```kotlin
class MyMiddleware : Middleware {
    override fun preRequest(request: Request): MiddlewareResponse {
        return MiddlewareResponse(true, "Failed.", HttpResponseStatus.OK)
    }
}
```

# Example
```kotlin
data class Test(val name: String, val age: Int)

@Path("/animals")
@SuppressWarnings("unused")
class TestListener : Controller() {

    @GET("/dog")
    fun dog(request: Request, @Query("age") age: Int, @Query("name") name: String): Response = json(
        Test(name, age), headers = mapOf(
            Cookie.createCookie("test", "test")
        )
    )

    @GET("/cat")
    fun cat(request: Request): Response = redirect("https://google.com/")

    @POST("/parrot")
    fun parrot(request: Request): Response = json(Test(request.body, 1337))

    @GET("/parrot")
    fun parrotGet(request: Request): Response = json(Test(request.body, 17))

}

// Bootstrap
fun main() {
    val backend = BackendManager()

    backend.routing.lazyRoute("/") {
        plain("Hello World!")
    }

    backend.routing.initializeController(TestListener())

    backend.run(3000)
}
```
