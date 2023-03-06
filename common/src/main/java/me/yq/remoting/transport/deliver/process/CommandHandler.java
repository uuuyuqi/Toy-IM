package me.yq.remoting.transport.deliver.process;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.yq.remoting.transport.command.RemotingCommand;
import me.yq.remoting.transport.deliver.CommandDispatcher;

/**
 * 最后的 command 入站处理器，负责将消息分发给业务侧
 * @author yq
 * @version v1.0 2023-02-17 2:41 PM
 */
@ChannelHandler.Sharable
public class CommandHandler extends ChannelInboundHandlerAdapter {

    private final CommandDispatcher dispatcher;

    public CommandHandler(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RemotingCommand){
            RemotingCommand command = (RemotingCommand) msg;
            dispatcher.dispatch(ctx.channel(),command);
        }
        else {
            super.channelRead(ctx, msg);
        }
    }

}