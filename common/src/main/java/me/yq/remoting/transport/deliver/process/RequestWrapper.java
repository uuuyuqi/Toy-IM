package me.yq.remoting.transport.deliver.process;

import me.yq.remoting.transport.support.BaseRequest;
import io.netty.channel.Channel;

/**
 * 消息包装，该系统在设计上，很多时候是需要服务端得知是哪个 channel 发来的请求。
 * 比如用户登陆，这就需要服务端记录发来请求的 channel，并存储到内存中。
 * 该任务会被提交给管理器类 {@link RequestProcessorManager}，做真正的业务任务分发
 * @author yq
 * @version v1.0 2023-02-16 3:01 PM
 */
public class RequestWrapper {

    private final BaseRequest request;

    /**
     * 服务端处理器得到的 客户端 channel ，很多需要需要这个 channel，比如 登陆、消息收发等
     */
    private final Channel clientChannel;

    public RequestWrapper(BaseRequest request, Channel clientChannel) {
        this.request = request;
        this.clientChannel = clientChannel;
    }


    public BaseRequest getRequest() {
        return request;
    }

    public Channel getClientChannel() {
        return clientChannel;
    }
}
