package me.yq.remoting.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.support.ChannelAttributes;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * @author yq
 * @version v1.0 2023-03-13 10:02
 */
@Sharable
public class ConnectionHandler extends ChannelDuplexHandler {

    private final ServerSessionMap serverSessionMap;

    public ConnectionHandler(ServerSessionMap serverSessionMap) {
        this.serverSessionMap = serverSessionMap;
    }

    /**
     * 当检测到 session 断开，就将其从 session 列表中移除
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelAttributes.ChannelState channelState = ctx.channel().attr(ChannelAttributes.CHANNEL_STATE).get();
        // 非 IM 连接，则无需处理 （IM 连接一定存在 channelState 属性）
        if (channelState == null)
            return;


        // 到这一步，说明该链接是意外断开，需要手工 remove
        serverSessionMap.removeSessionUnSafe(ctx.channel());
    }
}
