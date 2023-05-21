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
import me.yq.remoting.connection.ClientSideConnectionHandler;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.transport.Callback;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.transport.RequestFutureMap;
import me.yq.remoting.transport.Session;
import me.yq.remoting.transport.process.CommandHandler;
import me.yq.remoting.utils.NamedThreadFactory;
import me.yq.support.ChatClient;

import java.net.InetSocketAddress;


@Slf4j
public class RemotingClient {


    private final ChatClient client;

    private Session serverSession;

    private Bootstrap clientBootstrap;

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
            ClientSideConnectionHandler clientSideConnectionHandler = new ClientSideConnectionHandler(this.client);
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
                    pipeline.addLast("ClientSideConnectionHandler", clientSideConnectionHandler);
                }
            });
            clientBootstrap = bootstrap;

            connectToServer();

        } catch (Exception e) {
            throw new RuntimeException("客户端服务启动失败，错误原因: " + e);
        }

    }

    public void shutdown(long timeoutMillis) {

        if (timeoutMillis < 0)
            throw new RuntimeException("非法的优雅关闭超时时间: " + timeoutMillis);

        Channel serverChannel = serverSession.getChannel();
        RequestFutureMap requestFutureMap = serverChannel.attr(ChannelAttributes.CHANNEL_REQUEST_FUTURE_MAP).get();
        requestFutureMap.removeAllFuturesSafe(timeoutMillis);

        serverChannel.close();
        workerGroup.shutdownGracefully();
    }

    /**
     * 连接服务端。可能时首次连接，也可能时重连。
     */
    public void connectToServer() {
        if (clientBootstrap == null) {
            throw new IllegalStateException("客户端尚未启动，请先启动客户端！！！");
        }

        // 如果老连接可用，则不需要建联
        if (serverSession != null) {
            if (serverSession.isConnected()) {
                log.warn("当前已经连接到服务端，无需重复连接！");
                return;
            } else {
                Channel serverChannel = serverSession.getChannel();
                if (serverChannel != null)
                    serverChannel.close();
            }

        }


        String serverIP = client.getConfig().getValue(ClientConfigNames.REMOTE_SERVER_HOST);
        int serverPort = client.getConfig().getInt(ClientConfigNames.REMOTE_SERVER_PORT);
        ChannelFuture channelFuture = this.clientBootstrap.connect(new InetSocketAddress(serverIP, serverPort));
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
        Channel serverChannel = channelFuture.channel();
        this.serverSession = new Session(channelFuture.channel());
        // 心跳计数置 0
        serverChannel.attr(ChannelAttributes.HEARTBEAT_COUNT).set(0);
    }

    /**
     * 检测和服务端是否正常建联
     *
     * @return 和服务端的长连接异常情况下则返回 false，其他均为 true
     */
    public boolean hasConnected() {
        return serverSession != null && serverSession.isConnected();
    }


    public void sendRequestCallback(BaseRequest request, Callback callback) {
        CommandSendingDelegate.sendRequestCallback(
                this.serverSession.getChannel(),
                request,
                client.getConfig().getLong(ClientConfigNames.WAIT_RESPONSE_MILLIS),
                callback);
    }


    public BaseResponse sendRequestSync(BaseRequest request) {
        return CommandSendingDelegate.sendRequestSync(
                this.serverSession.getChannel(),
                request,
                client.getConfig().getLong(ClientConfigNames.WAIT_RESPONSE_MILLIS));
    }

    public void sendRequestOneway(BaseRequest request) {
        CommandSendingDelegate.sendRequestOneway(
                this.serverSession.getChannel(),
                request,
                client.getConfig().getLong(ClientConfigNames.WAIT_RESPONSE_MILLIS));
    }

    public Session getServerSession() {
        return serverSession;
    }
}
