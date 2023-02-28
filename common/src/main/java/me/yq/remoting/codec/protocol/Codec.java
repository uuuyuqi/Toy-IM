package me.yq.remoting.codec.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.util.List;

/**
 * 编解码器顶层抽象
 * @author yq
 * @version v1.0 2023-02-13 11:40 AM
 */
public interface Codec {
    /*
     * 这个看起来有点诡异而且不匹配的 encode 和 decode 参数，完全是为了使用和适配 netty 支持的带缓存的编解码
     */
    void encode(Serializable in, ByteBuf out);

    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out);

}
