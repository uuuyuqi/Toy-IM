package me.yq.remoting.codec.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import me.yq.remoting.command.RemotingCommand;

import java.util.List;

/**
 * 协议编解码器<br/>
 * 由于编解码器二合一，则会有一个问题：这个编解码器应该放在哪里？需要保证：<br/>
 * 1. 消息发送时，第一个出站是本编解码器<br/>
 * 2. 消息接收时，能够在合适的位置是本解码器<br/>
 * <font color="red"><b>must not be @Sharable<b/><font/>
 * @author yq
 * @version v1.0 2023-02-14 10:46 AM
 */
public class ProtocolCodec extends ByteToMessageCodec<RemotingCommand> {

    private final Codec codec;

    public ProtocolCodec(){
        this(null);
    }

    public ProtocolCodec(Codec defaultCodec){
        this.codec = defaultCodec != null ? defaultCodec : new YQCommandCodec();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingCommand msg, ByteBuf out) {
        codec.encode(msg,out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        codec.decode(ctx,in,out);
    }
}
