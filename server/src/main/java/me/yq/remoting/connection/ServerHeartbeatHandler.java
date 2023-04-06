package me.yq.remoting.connection;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.command.HeartbeatAckCommand;
import me.yq.remoting.command.HeartbeatCommand;

/**
 * 服务端心跳响应处理器，主要处理接收到来自客户端的心跳请求，并按照相同 id 对其响应
 * @author yq
 * @version v1.0 2023-03-07 21:49
 */
@Slf4j
@ChannelHandler.Sharable
public class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HeartbeatCommand){
            HeartbeatCommand heartbeat = (HeartbeatCommand) msg;
            HeartbeatAckCommand ack = new HeartbeatAckCommand(heartbeat.getMessageId());
            ctx.writeAndFlush(ack).addListener(
                    future -> {
                        if (!future.isSuccess()){
                            log.error("心跳响应失败！");
                        }
                    }
            );
        }
        else
            super.channelRead(ctx, msg);
    }
}
