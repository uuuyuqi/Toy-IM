package common;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.yq.remoting.support.Config;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 可以阻塞一切 IO 请求的 handler supplier，用于测试时模拟服务端 hang 住的情况。(cpu 打满)
 * 支持动态修改 config 配置，达到热生效的效果。
 * @author yq
 * @version v1.0 2023-04-06 15:51
 */
public class BlockAllHandlerSupplier implements Supplier<ChannelHandler> {


    private final Config config;

    public BlockAllHandlerSupplier(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    private final ChannelInboundHandler blockAllHandler = new ChannelInboundHandlerAdapter() {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            while (config.getBoolean(TestConfigNames.BLOCK_ALL_IO_THREADS)){
                TimeUnit.SECONDS.sleep(1);
            }

            ctx.fireChannelRead(msg);
        }
    };

    @Override
    public ChannelHandler get() {
        return blockAllHandler;
    }
}
