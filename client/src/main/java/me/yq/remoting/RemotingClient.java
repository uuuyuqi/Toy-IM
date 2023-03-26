package me.yq.remoting;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.config.ClientConfig;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.support.RequestFutureMap;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.utils.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;


@Slf4j
public class RemotingClient {

    private Channel serverChannel;

    /**
     * 定制化的 handler，如果该 handler 为空，则构建 通信层时 采用默认的 handler 做处理。<br/>
     * 这个 handlers 集合会被 {@link RemotingClient#registerHandlerInOrder(String name, ChannelHandler handler)} 注入，
     * 在组装 通信客户端 时，会优先以该结构为准，参考 {@link RemotingClient#start()}
     */
    private final LinkedHashMap<String, ChannelHandler> customHandlers = new LinkedHashMap<>();


    public RemotingClient(LinkedHashMap<String, ChannelHandler> customHandlers) {
        this.customHandlers.putAll(Objects.requireNonNull(customHandlers));
    }

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new NamedThreadFactory("Client-Worker", false)
    );


    public synchronized void registerHandlerInOrder(String name, ChannelHandler handler) {
        if (name == null || name.trim().equals(""))
            name = handler.getClass().getSimpleName();
        customHandlers.put(name, handler);
    }

    /**
     * 启动服务
     */
    public void start() {
        // 启动时一定需要 try-catch，netty 在启动时随时可能会报错！！！
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(workerGroup);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3 * 1000); // 建联超时时间 3 秒
            bootstrap.option(ChannelOption.TCP_NODELAY, true); // 禁止粘包
            bootstrap.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true)); // 默认使用 1.池化 2.直接 mem
            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (customHandlers.size() > 0) {
                        Set<String> handlerNames = customHandlers.keySet();
                        for (String handlerName : handlerNames) {
                            ChannelHandler handler = customHandlers.get(handlerName);
                            pipeline.addLast(handlerName, handler);
                        }
                    }
                }
            });

            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(ClientConfig.REMOTE_SERVER_HOST, ClientConfig.REMOTE_SERVER_PORT));
            channelFuture.addListener(
                    future -> {
                        if (!future.isSuccess()) {
                            log.error("连接服务器失败! 错误原因: " + future.cause().getMessage());
                            throw new RuntimeException("启动时连接服务端失败！！！");
                        } else {
                            log.info("连接服务器成功!");
                        }
                    }
            );

            // block to wait
            channelFuture.awaitUninterruptibly();
            this.serverChannel = channelFuture.channel();
        } catch (Exception e) {
            throw new RuntimeException("客户端服务启动失败，错误原因: " + e);
        }

    }

    public void shutdown(long timeoutMillis) {

        if (timeoutMillis < 0)
            throw new RuntimeException("非法的优雅关闭超时时间: " + timeoutMillis);

        RequestFutureMap requestFutureMap = serverChannel.attr(ChannelAttributes.REQUEST_FUTURE_MAP).get();
        requestFutureMap.removeAllFuturesSafe(timeoutMillis);

        serverChannel.close();
        workerGroup.shutdownGracefully();
    }


    public BaseResponse sendRequest(BaseRequest request) {
        return CommandSendingDelegate.sendRequestSync(this.serverChannel, request);
    }
}
