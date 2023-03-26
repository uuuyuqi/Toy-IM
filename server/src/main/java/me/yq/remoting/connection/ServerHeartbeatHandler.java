package me.yq.remoting.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.command.HeartbeatAckCommand;
import me.yq.remoting.command.HeartbeatCommand;
import me.yq.remoting.session.ServerSessionMap;

/**
 * 服务端对心跳请求的处理
 * @author yq
 * @version v1.0 2023-03-07 21:49
 */
@Slf4j
@ChannelHandler.Sharable
public class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private final ServerSessionMap serverSessionMap = ServerSessionMap.INSTANCE;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            Channel offlineChannel = ctx.channel();
            Long offlineUserId = serverSessionMap.removeSessionUnSafe(offlineChannel);
            log.info("检测到 id 为[{}]的用户已经失去连接，现在从在线列表中将其摘除", offlineUserId);
        }
        else
            super.userEventTriggered(ctx, evt);
    }

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
