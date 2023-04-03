package me.yq.remoting.support.session;


import io.netty.channel.Channel;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.support.RequestFutureMap;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话对象，包装了 channel，一个会话代表了一个 tcp 连接。在实际使用中，这个连接既可以是客户端连接，也可以是服务端连接。
 * 如果是客户端使用，只会维持和服务端一个 channel 的 session；如果是服务端使用，则需要用 map 缓存所有的客户端 session。
 * @author yq
 * @version v1.0 2023-03-21 20:31
 */
public class Session {

    private final int sessionId = idGenerator.incrementAndGet();

    private final static AtomicInteger idGenerator = new AtomicInteger(0);

    private final long uid;

    private final Channel channel;

    public Session(Channel channel){
        this(-1,channel);
    }

    public Session(long uid, Channel channel) {
        // pre process
        channel.attr(ChannelAttributes.CHANNEL_REQUEST_FUTURE_MAP).set(new RequestFutureMap());
        channel.attr(ChannelAttributes.CHANNEL_STATE).set(ChannelAttributes.ChannelState.CAN_REQUEST);

        this.uid = uid;
        this.channel = channel;
    }


    public int getSessionId() {
        return sessionId;
    }

    public long getUid() {
        return uid;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean notEquals(Object o){
        return !equals(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return uid == session.uid && Objects.equals(channel, session.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, channel);
    }
}
