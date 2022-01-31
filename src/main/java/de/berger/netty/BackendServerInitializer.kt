package de.berger.netty

import de.berger.RestApp
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpContentCompressor
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec

/**
 * Initializes the channel and adds properties to the pipeline.
 */
class BackendServerInitializer(private val manager: RestApp) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(channel: SocketChannel) {
        val pipeline = channel.pipeline()

        pipeline.addLast("decoder", HttpServerCodec())
        pipeline.addLast("aggregator", HttpObjectAggregator(10 * 1024 * 1024))
        pipeline.addLast("compress", HttpContentCompressor())
        pipeline.addLast("serverBusinessHandler", HttpTrafficHandler(manager))
    }
}