package me.yq.remoting;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.config.ServerConfig;
import me.yq.remoting.utils.NamedThreadFactory;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * chat server 的服务端通信层 <br/>
 * @author yq
 * @version v1.0 2023-02-22 11:36
 */
@Slf4j
public class RemotingServer{

    /**
     * pipeline 处理器
     */
    private final LinkedHashMap<String, ChannelHandler> customHandlers = new LinkedHashMap<>();

    private final NioEventLoopGroup boss = new NioEventLoopGroup(
            1,
            new NamedThreadFactory("Server-Boss-Thread", false));
    private final NioEventLoopGroup worker = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() + 1,
            new NamedThreadFactory("Server-Worker-Thread", false));


    private  Channel serverChannel;

    public RemotingServer(LinkedHashMap<String, ChannelHandler> customHandlers) {
        this.customHandlers.putAll(Objects.requireNonNull(customHandlers));
    }

    /**
     * 启动通信客户端
     */
    public void start() {
        io.netty.bootstrap.ServerBootstrap bootstrap = new io.netty.bootstrap.ServerBootstrap();
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(boss, worker);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ServerConfig.CONNECT_TIMEOUT_MILLIS); // 建联超时时间 3 秒
        bootstrap.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true)); // 默认使用 1.池化 2.直接 mem
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);  // 禁止粘包
        bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                Set<String> handlerNames = customHandlers.keySet();
                for (String handlerName : handlerNames) {
                    ChannelHandler handler = customHandlers.get(handlerName);
                    pipeline.addLast(handlerName, handler);
                }
            }
        });

        ChannelFuture channelFuture = bootstrap.bind(ServerConfig.SERVER_PORT);
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
