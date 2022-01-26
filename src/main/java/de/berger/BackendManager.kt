package de.berger

import de.berger.netty.BackendServerInitializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpResponseStatus

data class Test(val name: String, val age: Int)

@Path("/animals")
class TestListener : Controller() {

    @GET("/dog")
    fun dog(request: Request): Response = json(Test("Dog", 12))

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

/**
 * The backend manager (main class)
 */
class BackendManager {

    /**
     * Http constants
     */
    var port = 3000

    /**
     * Handling our routes
     */
    val routing = RouteHandler()

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
        this.port = port

        this.launchNettyServer(port)
    }

}