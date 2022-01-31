package de.berger.netty

import de.berger.RestApp
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil

/**
 * This class takes care of all incoming requests.
 */
class HttpTrafficHandler(private val manager: RestApp) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
            try {
                manager.routing.getResponse(manager.routing.transformRequest(msg)).let {
                    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, it.status)

                    response.content().writeBytes(Unpooled.copiedBuffer(it.body, CharsetUtil.UTF_8))

                    // Printing the request
                    println("[REQUEST] ${msg.method()} ${msg.uri()}")

                    response.headers().add(HttpHeaderNames.CONTENT_TYPE, it.type.contentType)
                    it.headers.forEach { (key, value) -> response.headers().add(key, value) }

                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            msg.release()
        }
    }

}