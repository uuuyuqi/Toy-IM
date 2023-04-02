package me.yq.remoting;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.codec.protocol.ProtocolCodec;
import me.yq.remoting.config.ClientConfigNames;
import me.yq.remoting.connection.ClientHeartbeatHandler;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.support.RequestFutureMap;
import me.yq.remoting.support.session.Session;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.transport.process.CommandHandler;
import me.yq.remoting.utils.NamedThreadFactory;
import me.yq.support.ChatClient;

import java.net.InetSocketAddress;


@Slf4j
public class RemotingClient {


    private final ChatClient client;

    private Session serverSession;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new NamedThreadFactory("Client-Worker", true)
    );


    public RemotingClient(ChatClient chatClient) {
        this.client = chatClient;
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

            LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);
            ClientHeartbeatHandler heartbeatHandler = new ClientHeartbeatHandler(this.client);
            CommandHandler commandHandler = new CommandHandler(this.client.getUserProcessor());
            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("LoggingHandler", loggingHandler);
                    pipeline.addLast("ProtocolCodec", new ProtocolCodec());
                    if (client.getConfig().getBoolean(ClientConfigNames.HEARTBEAT_ENABLE)){
                        Integer idleSeconds = client.getConfig().getInt(ClientConfigNames.HEARTBEAT_IDLE_SECONDS);
                        pipeline.addLast("IdleStateHandler", new IdleStateHandler(0, 0, idleSeconds));
                        pipeline.addLast("ServerHeartbeatHandler", heartbeatHandler);
                    }
                    pipeline.addLast("CommandHandler", commandHandler);
                }
            });

            String serverIP = client.getConfig().getValue(ClientConfigNames.REMOTE_SERVER_HOST);
            int serverPort = client.getConfig().getInt(ClientConfigNames.REMOTE_SERVER_PORT);
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(serverIP,serverPort));
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
            this.serverSession = new Session(channelFuture.channel());
        } catch (Exception e) {
            throw new RuntimeException("客户端服务启动失败，错误原因: " + e);
        }

    }

    public void shutdown(long timeoutMillis) {

        if (timeoutMillis < 0)
            throw new RuntimeException("非法的优雅关闭超时时间: " + timeoutMillis);

        Channel serverChannel = serverSession.getChannel();
        RequestFutureMap requestFutureMap = serverChannel.attr(ChannelAttributes.REQUEST_FUTURE_MAP).get();
        requestFutureMap.removeAllFuturesSafe(timeoutMillis);

        serverChannel.close();
        workerGroup.shutdownGracefully();
    }



    public BaseResponse sendRequest(BaseRequest request) {
        return CommandSendingDelegate.sendRequestSync(this.serverSession.getChannel(), request);
    }
}
