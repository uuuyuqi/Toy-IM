package me.yq.remoting.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import me.yq.support.ChatClient;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * 连接处理器，用于检测和服务端连接的状态
 * @author yq
 * @version v1.0 2023-04-14 18:33
 */
@Slf4j
@Sharable
public class ClientSideConnectionHandler extends ChannelInboundHandlerAdapter {

    private final ChatClient chatClient;

    public ClientSideConnectionHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel() == chatClient.getServerSession().getChannel()){
            log.warn("检测到和服务端的连接已经断开！");
            chatClient.loseConnection();
        }
        super.channelInactive(ctx);
    }
}
