package me.yq.remoting;

import me.yq.remoting.config.ClientConfig;
import me.yq.support.ClientBootstrap;
import me.yq.remoting.codec.protocol.ProtocolCodec;
import me.yq.remoting.utils.NamedThreadFactory;
import me.yq.remoting.transport.deliver.CommandDispatcher;
import me.yq.remoting.transport.deliver.CommandSendingDelegate;
import me.yq.remoting.transport.deliver.process.CommandHandler;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;


@Slf4j
public class RemotingClient {

    private Channel serverChannel;

    private final ClientBootstrap clientBootstrap;

    private final CommandHandler clientHandler;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new NamedThreadFactory("Remoting-Client", false)
    );


    public RemotingClient(ClientBootstrap clientBootstrap) {

        this.clientBootstrap = clientBootstrap;

        RequestProcessorManager processorManager = clientBootstrap.getProcessorManager();
        CommandDispatcher dispatcher = new CommandDispatcher(processorManager);

        this.clientHandler = new CommandHandler(dispatcher);
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
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    ch.pipeline().addLast(new ProtocolCodec());
                    ch.pipeline().addLast(clientHandler);
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


    public BaseResponse sendRequest(BaseRequest request) {
        CommandSendingDelegate sendingDelegate = this.clientBootstrap.getSendingDelegate();
        return sendingDelegate.sendRequestSync(this.serverChannel, request);
    }

}
