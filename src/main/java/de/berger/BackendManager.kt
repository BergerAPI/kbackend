package de.berger

import de.berger.netty.BackendServerInitializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpResponseStatus

data class Test(val name: String, val age: Int)

@Path("/animals")
@SuppressWarnings("unused")
class TestListener : Controller() {

    @GET("/dog")
    fun dog(request: Request, @Query("age") age: Int): Response = json(Test("Dog", age))

    @GET("/cat")
    fun cat(request: Request): Response = json(Test("Cat", 69))

    @GET("/parrot")
    fun parrot(request: Request): Response = json(Test("Parrot", 1337))

}

/**
 * This middleware always fails, so we can test the middleware handling.
 */
class AutoFailMiddleware : Middleware {
    override fun preRequest(request: Request): MiddlewareResponse =
        MiddlewareResponse(true, "This is a middleware that always fails", HttpResponseStatus.BAD_REQUEST)
}

// Bootstrap
fun main() {
    val backend = BackendManager()

    backend.routing.lazyRoute("/") {
        plain("Hello World!")
    }

    backend.routing.initializeController(TestListener())
    backend.middleware.addMiddleware(AutoFailMiddleware())

    backend.run(3000)
}

/**
 * The backend manager (main class)
 */
class BackendManager {

    /**
     * Handling our routes
     */
    val routing = RouteHandler(this)

    /**
     * Handling our middlewares
     */
    val middleware = MiddlewareHandler()

    /**
     * Starting the listening netty server, which will be used to accept incoming requests.
     */
    private fun launchNettyServer(port: Int) {
        val boss = NioEventLoopGroup(1)
        val worker = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()

            serverBootstrap.group(boss, worker).channel(NioServerSocketChannel::class.java)
                .childHandler(BackendServerInitializer(this))

            val channel = serverBootstrap.bind(port).sync().channel()

            channel.closeFuture().sync()
        } finally {
            boss.shutdownGracefully()
            worker.shutdownGracefully()
        }
    }

    /**
     * This method is used to start the backend.
     */
    fun run(port: Int) {
        this.launchNettyServer(port)
    }

}