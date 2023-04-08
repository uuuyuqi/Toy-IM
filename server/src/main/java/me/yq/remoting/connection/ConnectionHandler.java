package me.yq.remoting.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.ChannelAttributes;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * @author yq
 * @version v1.0 2023-03-13 10:02
 */
@Sharable
public class ConnectionHandler extends ChannelDuplexHandler {

    private final SessionMap sessionMap;

    public ConnectionHandler(SessionMap sessionMap) {
        this.sessionMap = sessionMap;
    }

    /**
     * 当检测到 session 断开，就将其从 session 列表中移除
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        // 非 IM 连接，则无需处理 （IM 连接一定存在 channelState 属性）
        // 不过一般情况下，这个端口下的连接都是 IM 连接
        if (!isIMConnection(ctx))
            return;

        // 到这一步，说明该链接是意外断开，需要手工 remove
        sessionMap.removeSessionUnSafe(ctx.channel());
        ctx.close();

        // 保证其他 channelHandler 也能收到通知并进行对应的处理
        super.channelInactive(ctx);
    }

    /**
     * 判断是否是 IM 连接。当前的判断策略是：只要存在 channelState 属性，就认为是 IM 连接
     * @param ctx channel 上下文
     * @return 是 IM 连接则返回 true
     */
    private boolean isIMConnection(ChannelHandlerContext ctx){
        return ctx.channel().attr(ChannelAttributes.CHANNEL_STATE).get() != null;
    }

}
