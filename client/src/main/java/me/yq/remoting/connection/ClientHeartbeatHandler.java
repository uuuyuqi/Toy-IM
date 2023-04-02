package me.yq.remoting.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.command.HeartbeatAckCommand;
import me.yq.remoting.command.HeartbeatCommand;
import me.yq.remoting.config.ClientConfigNames;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.support.ChatClient;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * heartbeat 处理器，采用专门的 handler 处理。
 * 个人认为心跳是一个非常快速的行为，没有必要提交到 biz 线程池中。
 * 此外，心跳只是客户端的行为，和服务端无关！服务端也会检测空闲，但是达到一定时间后，就会强行断开客户端连接
 *
 * @author yq
 * @version v1.0 2023-03-07 17:49
 */
@Sharable
@Slf4j
public class ClientHeartbeatHandler extends ChannelInboundHandlerAdapter {


    private final ChatClient chatClient;


    public ClientHeartbeatHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // 触发心跳
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Integer maxFailCount = chatClient.getConfig().getInt(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT);
            Attribute<Integer> count = ctx.channel().attr(ChannelAttributes.HEARTBEAT_COUNT);
            // 首次心跳
            if (count.get() == null)
                count.set(0);

            if (count.get() >= maxFailCount) {
                log.error("{}次心跳失败！已经和服务端断开连接，现在关闭客户端......", maxFailCount);
                chatClient.loseConnection();
            }

            log.info("检测到空闲，开始发送心跳！");
            HeartbeatCommand heartbeat = new HeartbeatCommand();
            ctx.channel().attr(ChannelAttributes.EXPECTED_HEARTBEAT_RESPONSE_ID).set(heartbeat.getMessageId());
            count.set(count.get() + 1);

            ctx.writeAndFlush(heartbeat).addListener(
                    future -> {
                        if (!future.isSuccess()) {
                            log.error("心跳发送失败");
                        }
                    });
        } else
            super.userEventTriggered(ctx, evt);
    }

    // 分析心跳返回
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HeartbeatAckCommand) {
            HeartbeatAckCommand ack = (HeartbeatAckCommand) msg;
            int ackId = ack.getMessageId();

            Integer expectedId = ctx.channel().attr(ChannelAttributes.EXPECTED_HEARTBEAT_RESPONSE_ID).get();
            if (expectedId == null) {
                log.warn("意外收到心跳响应，可能服务端重复响应？");
                super.channelRead(ctx, msg);
                return;
            }


            if (expectedId == ackId) {
                log.info("心跳成功！");
                ctx.channel().attr(ChannelAttributes.EXPECTED_HEARTBEAT_RESPONSE_ID).set(null);
                ctx.channel().attr(ChannelAttributes.HEARTBEAT_COUNT).set(0);
            } else {
                log.info("心跳响应不匹配！");
                Integer count = ctx.channel().attr(ChannelAttributes.HEARTBEAT_COUNT).get();
                ctx.channel().attr(ChannelAttributes.HEARTBEAT_COUNT).set(count + 1);
            }


        } else
            super.channelRead(ctx, msg);
    }


}
