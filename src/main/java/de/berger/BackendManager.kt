package de.berger

import de.berger.cookie.Cookie
import de.berger.netty.BackendServerInitializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

/**
 * The backend manager (main class)
 */
class RestApp {

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