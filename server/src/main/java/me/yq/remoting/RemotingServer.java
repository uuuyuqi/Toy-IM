package me.yq.remoting;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.codec.protocol.ProtocolCodec;
import me.yq.remoting.config.ServerConfig;
import me.yq.remoting.transport.deliver.CommandDispatcher;
import me.yq.remoting.transport.deliver.process.CommandHandler;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;
import me.yq.remoting.utils.NamedThreadFactory;

/**
 * chat server 的服务端通信层
 *
 * @author yq
 * @version v1.0 2023-02-22 11:36
 */
@Slf4j
public class RemotingServer {

    private final CommandHandler serverHandler;

    private final NioEventLoopGroup boss = new NioEventLoopGroup(
            1,
            new NamedThreadFactory("Server-Boss-Thread", false));
    private final NioEventLoopGroup worker = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() + 1,
            new NamedThreadFactory("Server-Worker-Thread", false));


    public RemotingServer(RequestProcessorManager processorManager) {
        CommandDispatcher dispatcher = new CommandDispatcher(processorManager);
        this.serverHandler = new CommandHandler(dispatcher);
    }

    public void start() {
        io.netty.bootstrap.ServerBootstrap bootstrap = new io.netty.bootstrap.ServerBootstrap();
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(boss, worker);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3 * 1000); // 建联超时时间 3 秒
        bootstrap.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true)); // 默认使用 1.池化 2.直接 mem
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);  // 禁止粘包
        bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast("LoggingHandler",new LoggingHandler(LogLevel.DEBUG));
                ch.pipeline().addLast("ProtocolCodec",new ProtocolCodec());
                ch.pipeline().addLast("CommandHandler",serverHandler);
            }
        });

        bootstrap.bind(ServerConfig.SERVER_PORT).addListener(
                future -> {
                    if (!future.isSuccess())
                        log.error("服务器启动失败......");
                    else {
                        log.info("服务器启动成功!");
                    }
                }
        );
    }

    public void shutdown() {
        this.boss.shutdownGracefully();
        this.worker.shutdownGracefully();
        log.info("服务端已关闭!");
    }


}
