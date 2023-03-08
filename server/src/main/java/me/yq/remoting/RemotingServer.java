package me.yq.remoting;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.codec.protocol.ProtocolCodec;
import me.yq.remoting.config.ServerConfig;
import me.yq.remoting.transport.deliver.CommandDispatcher;
import me.yq.remoting.transport.deliver.RequestRecord;
import me.yq.remoting.transport.deliver.heartbeat.ServerHeartbeatHandler;
import me.yq.remoting.transport.deliver.process.CommandHandler;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;
import me.yq.remoting.utils.NamedThreadFactory;

import java.util.concurrent.TimeUnit;

/**
 * chat server 的服务端通信层
 *
 * @author yq
 * @version v1.0 2023-02-22 11:36
 */
@Slf4j
public class RemotingServer {

    private final CommandHandler serverHandler;

    private final RequestRecord requestRecord;

    private final NioEventLoopGroup boss = new NioEventLoopGroup(
            1,
            new NamedThreadFactory("Server-Boss-Thread", false));
    private final NioEventLoopGroup worker = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() + 1,
            new NamedThreadFactory("Server-Worker-Thread", false));


    public RemotingServer(RequestProcessorManager processorManager) {
        CommandDispatcher dispatcher = new CommandDispatcher(processorManager);
        this.serverHandler = new CommandHandler(dispatcher);
        this.requestRecord = RequestRecord.getInstance();
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
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LoggingHandler",new LoggingHandler(LogLevel.DEBUG));
                pipeline.addLast("ProtocolCodec",new ProtocolCodec());
                pipeline.addLast("CommandHandler",serverHandler);

                pipeline.addLast("IdleStateHandler",new IdleStateHandler(0,0,90));
                pipeline.addLast("ServerHeartbeatHandler",new ServerHeartbeatHandler());

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

    public void shutdown(long timeoutMillis) {

        if (requestRecord.hasTasks()){
            try {
                TimeUnit.MILLISECONDS.sleep(timeoutMillis);
            } catch (InterruptedException ignored) {
                throw new IllegalStateException("发现有未处理的请求，但是处理线程已经关闭了！");
            }
        }
        if (requestRecord.hasTasks())
            log.warn("当前仍有未处理的请求，现在准备强制关闭！");

        this.boss.shutdownGracefully();
        this.worker.shutdownGracefully();
    }


}
