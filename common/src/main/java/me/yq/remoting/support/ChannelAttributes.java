package me.yq.remoting.support;

import io.netty.util.AttributeKey;

/**
 * @author yq
 * @version v1.0 2023-03-21 20:17
 */
public class ChannelAttributes {
    /**
     * 用来缓存请求任务，实现任务的乱序收发处理
     */
    public static AttributeKey<RequestFutureMap> CHANNEL_REQUEST_FUTURE_MAP = AttributeKey.valueOf("CHANNEL_REQUEST_FUTURE_MAP");

    /**
     * 用来表示 是否能接收来自 当前 channel 的请求。在优雅停机状态下，会设置为 false，但是可以给其发送响应。
     */
    public static final AttributeKey<ChannelState> CHANNEL_STATE = AttributeKey.valueOf("CHANNEL_STATE");

    /**
     * 当前心跳次数
     */
    public final static AttributeKey<Integer> HEARTBEAT_COUNT = AttributeKey.valueOf("HEARTBEAT_COUNT");

    /**
     * 当前期待的心跳响应 id
     */
    public final static AttributeKey<Integer> EXPECTED_HEARTBEAT_RESPONSE_ID = AttributeKey.valueOf("EXPECTED_HEARTBEAT_RESPONSE_ID");


    /**
     * 用来表示 channel 的状态 <br/>
     * 1. CAN_REQUEST：正常状态，可以接收请求<br/>
     * 2. CANNOT_REQUEST：当前系统不再接收 channel 发来的新请求，但是可以给其发送响应。一般发生在系统停机的时候<br/>
     * 3. CLOSED：当前 channel 已关闭，当系统发现 channel 处于该状态，可以考虑直接将其优雅关闭
     */
    public enum ChannelState {
        CAN_REQUEST,
        CANNOT_REQUEST,
        CLOSED,
    }
}
