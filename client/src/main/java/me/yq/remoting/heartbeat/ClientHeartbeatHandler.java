package me.yq.remoting.heartbeat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.transport.command.HeartbeatAckCommand;
import me.yq.remoting.transport.command.HeartbeatCommand;

import java.util.Stack;

/**
 * heartbeat 处理器，采用专门的 handler 处理。
 * 个人认为心跳是一个非常快速的行为，没有必要提交到 biz 线程池中。
 * 此外，心跳只是客户端的行为，和服务端无关！服务端也会检测空闲，但是达到一定时间后，就会强行断开客户端连接
 * @author yq
 * @version v1.0 2023-03-07 17:49
 */
@Slf4j
public class ClientHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final int MAX_HB_COUNT = 3;

    /**
     * 静态的目的是提高代码的可测试性
     */
    private static final Stack<Integer> sendStack = new Stack<>();

    public static int getHeartbeatCount(){
        return sendStack.size();
    }

    // 触发心跳
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {

            if (getHeartbeatCount() >= MAX_HB_COUNT){
                log.error("{}次心跳失败！已经和服务端断开连接，现在关闭客户端......",MAX_HB_COUNT);
                System.exit(-1);
            }

            log.info("检测到空闲，开始发送心跳！");
            HeartbeatCommand heartbeat = new HeartbeatCommand();
            sendStack.push(heartbeat.getMessageId());

            ctx.writeAndFlush(heartbeat).addListener(
                    future -> {
                        if (!future.isSuccess()) {
                            log.error("心跳发送失败");
                            sendStack.pop();
                        }
                    });
        } else
            super.userEventTriggered(ctx, evt);
    }

    // 分析心跳返回
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HeartbeatAckCommand){
            HeartbeatAckCommand ack = (HeartbeatAckCommand) msg;
            int ackId = ack.getMessageId();

            if (getHeartbeatCount() >=1 && sendStack.pop() == ackId){
                log.info("心跳成功！");
                sendStack.clear();
            }
            else{
                log.error("心跳响应序列号匹配失败，可能发生网络问题？即将开始重新心跳");
            }

        }
        else
            super.channelRead(ctx, msg);
    }


}
