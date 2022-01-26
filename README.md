# kbackend
A simple backend library for creating backends in Kotlin/Java

# Setup
This project is based on Apache Maven. So you can clone the project and open the cloned directory in IntelliJ. It will take care of all the setup processes for you.

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
