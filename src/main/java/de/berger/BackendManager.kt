package de.berger

import de.berger.cookie.Cookie
import de.berger.netty.BackendServerInitializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

data class Test(val name: String, val age: Int)

@Path("/animals")
@SuppressWarnings("unused")
class TestListener : Controller() {

    @GET("/dog")
    fun dog(request: Request, @Query("name") name: String, @Body(Test::class) body: Test): Response = json(
        Test(name, body.age), headers = mapOf(
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