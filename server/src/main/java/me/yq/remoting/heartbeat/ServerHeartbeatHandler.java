package me.yq.remoting.heartbeat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import me.yq.biz.SignOutRequest;
import me.yq.remoting.transport.command.DefaultRequestCommand;
import me.yq.remoting.transport.command.HeartbeatAckCommand;
import me.yq.remoting.transport.command.HeartbeatCommand;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.constant.BizCode;

/**
 * 服务端对心跳请求的处理
 * @author yq
 * @version v1.0 2023-03-07 21:49
 */
@Slf4j
public class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            Channel offlineChannel = ctx.channel();

            SignOutRequest signOutRequest = new SignOutRequest(offlineChannel);
            BaseRequest baseRequest = new BaseRequest(BizCode.SignOutRequest, signOutRequest);
            DefaultRequestCommand requestCommand = new DefaultRequestCommand();
            requestCommand.setAppRequest(baseRequest);
            requestCommand.toRemotingCommand();

            ctx.pipeline().fireChannelRead(signOutRequest);

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
