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


    public enum ChannelState {
        CAN_REQUEST, // 正常
        CANNOT_REQUEST, // 系统正在停机
        CLOSED, // 当前 channel 已关闭
    }
}
