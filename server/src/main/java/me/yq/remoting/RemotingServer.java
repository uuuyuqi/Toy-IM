package me.yq.remoting;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.codec.protocol.ProtocolCodec;
import me.yq.remoting.config.Config;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.connection.ConnectionHandler;
import me.yq.remoting.connection.ServerHeartbeatHandler;
import me.yq.remoting.connection.ServerIdleConnHandler;
import me.yq.remoting.transport.process.CommandHandler;
import me.yq.remoting.utils.NamedThreadFactory;
import me.yq.support.ChatServer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * chat server 的服务端通信层 <br/>
 *
 * @author yq
 * @version v1.0 2023-02-22 11:36
 */
@Slf4j
public class RemotingServer {


    private final ChatServer server;

    private final NioEventLoopGroup boss = new NioEventLoopGroup(
            1,
            new NamedThreadFactory("Server-Boss-Thread", false));

    private final NioEventLoopGroup worker = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() + 1,
            new NamedThreadFactory("Server-Worker-Thread", false));


    private Channel serverChannel;

    public RemotingServer(ChatServer chatServer) {
        this.server = chatServer;
    }


    /**
     * 启动通信客户端
     */
    public void start() {
        Config serverConfig = server.getConfig();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(boss, worker);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serverConfig.getInt(ServerConfigNames.CONNECT_TIMEOUT_MILLIS)); // 建联超时时间 3 秒
        bootstrap.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true)); // 默认使用 1.池化 2.直接 mem
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);  // 禁止粘包

        LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);
        ConnectionHandler connectionHandler = new ConnectionHandler(this.server.getSessionMap());
        ServerIdleConnHandler idleConnHandler = new ServerIdleConnHandler(this.server.getSessionMap());
        ServerHeartbeatHandler heartbeatHandler = new ServerHeartbeatHandler();
        CommandHandler commandHandler = new CommandHandler(this.server.getUserProcessor());
        bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                Map<String, Supplier<ChannelHandler>> handlersAhead = server.getCustomHandlersAhead();
                handlersAhead.forEach((name, supplier) -> pipeline.addLast(name, supplier.get()));

                pipeline.addLast("LoggingHandler", loggingHandler);
                pipeline.addLast("ConnectionHandler", connectionHandler);
                pipeline.addLast("ProtocolCodec", new ProtocolCodec());

                pipeline.addLast("ServerHeartbeatHandler", heartbeatHandler);

                if (serverConfig.getBoolean(ServerConfigNames.IDLE_CHECK_ENABLE)) {
                    Integer idleSeconds = serverConfig.getInt(ServerConfigNames.CLIENT_TIMEOUT_SECONDS);
                    pipeline.addLast("IdleStateHandler", new IdleStateHandler(0, 0, idleSeconds));
                    pipeline.addLast("ServerIdleConnHandler", idleConnHandler);
                }

                pipeline.addLast("CommandHandler", commandHandler);
            }
        });

        ChannelFuture channelFuture = bootstrap.bind(serverConfig.getInt(ServerConfigNames.SERVER_PORT));
        channelFuture.addListener(
                future -> {
                    if (!future.isSuccess())
                        log.error("服务器启动失败......");
                    else {
                        log.info("服务器启动成功!");
                    }
                }
        );

        this.serverChannel = channelFuture.channel();
    }

    /**
     * 关闭通信服务端
     */
    public void shutdown() {
        if (serverChannel != null)
            serverChannel.close();
        this.boss.shutdownGracefully();
        this.worker.shutdownGracefully();
    }


}
