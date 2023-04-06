package me.yq.remoting.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.session.ServerSessionMap;

/**
 * 服务端空闲检测处理器，主要检测是否存在客户端长时间没有数据来往。对于检测出来的客户端，直接将其从在线列表中摘除，并关闭连接。
 * @author yq
 * @version v1.0 2023-04-06 19:19
 */
@Slf4j
public class ServerIdleConnHandler extends ChannelInboundHandlerAdapter {

    private final ServerSessionMap serverSessionMap;

    public ServerIdleConnHandler(ServerSessionMap serverSessionMap) {
        this.serverSessionMap = serverSessionMap;
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            Channel offlineChannel = ctx.channel();
            Long offlineUserId = serverSessionMap.removeSessionUnSafe(offlineChannel);
            ctx.channel().close();
            log.info("检测到 id 为[{}]的用户已经失去连接，现在从在线列表中将其摘除，并将连接关闭", offlineUserId);
        }
        else
            super.userEventTriggered(ctx, evt);
    }
}
