# kbackend
A simple backend library for creating backends in Kotlin/Java

# Setup
This project is based on Apache Maven. So you can clone the project and open the cloned directory in IntelliJ. It will take care of all the setup processes for you.

# Example
````kotlin
@Path("/animals")
class TestListener : Controller() {

    @GET("/dog")
    fun dog(request: Request, @Query("age") age: Int): Response = json(Test("Dog", age))

    @GET("/cat")
    fun cat(request: Request): Response = json(Test("Cat", 69))

    @GET("/parrot")
    fun parrot(request: Request): Response = json(Test("Parrot", 1337))

}

// Bootstrap
fun main(args: Array<String>) {
    val backend = BackendManager()

    backend.routing.lazyRoute("/") {
        plain("Hello World!")
    }

    backend.routing.initializeController(TestListener())

    backend.run(3000)
}
```
