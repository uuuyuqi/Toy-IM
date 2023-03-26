package me.yq.remoting.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import me.yq.remoting.session.ServerSessionMap;

/**
 * @author yq
 * @version v1.0 2023-03-13 10:02
 */
public class ConnectionHandler extends ChannelDuplexHandler {

    private final ServerSessionMap serverSessionMap = ServerSessionMap.INSTANCE;


    /**
     * 当检测到 session 断开，就将其从 session 列表中移除
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 实际上，如果这么写，默认所有的连接都是 客户端 IM 连接，实际上未必
        // 但是实际大多数都是客户端的 IM 连接，影响不是太大
        serverSessionMap.removeSessionUnSafe(ctx.channel());
    }
}
