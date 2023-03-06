package me.yq.remoting.transport.deliver;

import io.netty.channel.Channel;
import me.yq.remoting.transport.command.DefaultRequestCommand;
import me.yq.remoting.transport.command.DefaultResponseCommand;
import me.yq.remoting.transport.command.RemotingCommand;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;

/**
 * 默认的消息收发器，业务服务端、客户端通用，主要用来做业务消息发送，应该满足进程唯一
 *
 * @author yq
 * @version v1.0 2023-02-17 10:15 AM
 */
public class CommandDispatcher {

    /**
     * 业务处理器集合
     */
    private final RequestProcessorManager processorManager;

    /**
     * 通信请求缓存
     */
    private final RequestRecord requestRecord = RequestRecord.getInstance();

    public CommandDispatcher(RequestProcessorManager processorManager) {
        this.processorManager = processorManager;
    }


    /**
     * 接受并分发消息，接受消息有 2 种情况：<br/>
     * - 发来请求<br/>
     * - 发来响应<br/>
     * 此外，本方法还有一个非常重要的行为，那就是登记注册收到的请求。
     *
     * @param channel 发来消息的 channel
     * @param command 发来的业务信息
     */
    public void dispatch(Channel channel, RemotingCommand command) {
        // 收到的是请求
        if (command instanceof DefaultRequestCommand) {
            DefaultRequestCommand requestCommand = (DefaultRequestCommand) command;
            processorManager.acceptAndProcess(channel, requestCommand);

        // 收到的是响应
        } else if (command instanceof DefaultResponseCommand) {
            DefaultResponseCommand responseCommand = (DefaultResponseCommand) command;
            // 将 response 传递给给请求处
            requestRecord.putResponseCommand(channel, command.getMessageId(), responseCommand);
        }
    }

}
