package common;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.yq.remoting.support.Config;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 可以拦截一切 IO 请求的 handler，模拟服务端丢弃请求的情况。(cpu 不会打满)
 * 支持动态修改 config 配置，达到热生效的效果。
 * @author yq
 * @version v1.0 2023-04-06 15:51
 */
public class InterceptAllHandlerSupplier implements Supplier<ChannelHandler> {


    private final Config config;

    public InterceptAllHandlerSupplier(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    private final ChannelInboundHandler interceptAllHandler = new ChannelInboundHandlerAdapter() {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (config.getBoolean(TestConfigNames.DISCARD_ALL_REQUESTS))
                return;

            ctx.fireChannelRead(msg);
        }
    };

    @Override
    public ChannelHandler get() {
        return interceptAllHandler;
    }
}
