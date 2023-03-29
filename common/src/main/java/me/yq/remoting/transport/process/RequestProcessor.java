package me.yq.remoting.transport.process;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;

/**
 * 请求处理器模板类
 *
 * @author yq
 * @version v1.0 2023-02-21 14:13
 */
@Slf4j
public abstract class RequestProcessor {
    /**
     * processor 在做业务处理的时候可能会用到 channel 对象，比如用户登陆，就需要 processor 收集用户的 channel 信息
     */
    private final ThreadLocal<Channel> channelLocal = new ThreadLocal<>();

    /**
     * 处理业务请求
     * @param request 业务请求
     */
    abstract public BaseResponse process(BaseRequest request);

    public ThreadLocal<Channel> getChannelLocal() {
        return channelLocal;
    }
}
