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
    public static AttributeKey<RequestFutureMap> REQUEST_FUTURE_MAP = AttributeKey.valueOf("REQUEST_FUTURE_MAP");

    /**
     * 用来表示 是否能接收来自 当前 channel 的请求。在优雅停机状态下，会设置为 false，但是可以给其发送响应。
     */
    public static final AttributeKey<ChannelState> CHANNEL_STATE = AttributeKey.valueOf("CHANNEL_STATE");

    public enum ChannelState {
        CAN_REQUEST, // 正常
        CANNOT_REQUEST, // 系统正在停机
        CLOSED, // 当前 channel 已关闭
    }
}
