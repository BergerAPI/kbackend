package de.berger.netty

import de.berger.BackendManager
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil

/**
 * This class takes care of all incoming requests.
 */
class HttpTrafficHandler(private val manager: BackendManager) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
            try {
                manager.routing.getResponse(manager.routing.transformRequest(msg)).let {
                    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, it.status)

                    response.content().writeBytes(Unpooled.copiedBuffer(it.body, CharsetUtil.UTF_8))

                    // Printing the request
                    println("[REQUEST] ${msg.method} ${msg.uri}")

                    response.headers().add(HttpHeaderNames.CONTENT_TYPE, it.type.contentType)

                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            msg.release()
        }
    }

}