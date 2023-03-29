package me.yq.remoting.transport.process;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.command.RemotingCommand;


/**
 * 最后的 command 入站处理器，负责将消息分发给业务侧
 *
 * @author yq
 * @version v1.0 2023-02-17 2:41 PM
 */
@Slf4j
@ChannelHandler.Sharable
public class CommandHandler extends ChannelInboundHandlerAdapter {

    private final UserProcessor userProcessor;


    public CommandHandler(UserProcessor bizProcessor) {
        this.userProcessor = bizProcessor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof RemotingCommand)
            userProcessor.processCommand(ctx,(RemotingCommand)msg);

        // 其他类型的请求则直接放行
        else
            super.channelRead(ctx, msg);

    }



}