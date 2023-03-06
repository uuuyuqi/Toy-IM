package me.yq.remoting.transport.deliver;

import io.netty.channel.Channel;
import me.yq.remoting.transport.command.DefaultRequestCommand;
import me.yq.remoting.transport.command.DefaultResponseCommand;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;

/**
 * 消息发送委托
 *
 * @author yq
 * @version v1.0 2023-02-22 17:56
 */
public class CommandSendingDelegate {

    static private final CommandSendingDelegate INSTANCE = new CommandSendingDelegate();

    private CommandSendingDelegate() {
    }

    public static CommandSendingDelegate getInstance() {
        return INSTANCE;
    }

    /**
     * 通信请求缓存
     */
    private final RequestRecord requestRecord = RequestRecord.getInstance();


    /**
     * 同步发送请求
     *
     * @param channel 接收消息的 channel
     * @param request 待发送的业务信息
     * @return 响应数据
     */
    public BaseResponse sendRequestSync(Channel channel, BaseRequest request) {
        DefaultRequestCommand requestCommand = createSerializedRemotingCommand(request);
        DefaultResponseCommand responseCommand = sendCommandSync(channel, requestCommand);
        if (responseCommand == null)
            return null;

        responseCommand.resolveRemoting(); // 反序列化
        return responseCommand.getAppResponse();
    }

    /**
     * 同步发送消息
     *
     * @param channel        接收消息的 channel
     * @param requestCommand 待发送的 command
     * @return 响应数据
     */
    private DefaultResponseCommand sendCommandSync(Channel channel, DefaultRequestCommand requestCommand) {
        try {
            requestRecord.registerTask(channel, requestCommand.getMessageId());
            try {
                channel.writeAndFlush(requestCommand).addListener(
                        future -> {
                            if (!future.isSuccess()) {
                                // fixme 编码的时候报了空指针
                                throw new RuntimeException("消息发送失败!  异常信息： " + future.cause().getMessage());
                            }
                        }
                );
            } catch (Exception e) {
                requestRecord.putFailedResponseCommand(channel, requestCommand.getMessageId(), e);
            }

            return requestRecord.getResponseCommand(channel, requestCommand.getMessageId());
        } finally {
            requestRecord.removeTask(channel, requestCommand.getMessageId());
        }
    }

    /**
     * 根据业务请求消息，生成一个已经序列化后的通信对象。该方法通常在即将进行远程通信时调用，
     * 可以将原始的业务请求对象，包装成一个通信对象。
     *
     * @param request 待包装的业务对象
     * @return 已经序列化后的远程通信对象
     */
    private DefaultRequestCommand createSerializedRemotingCommand(BaseRequest request) {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setAppRequest(request);
        // 序列化
        requestCommand.toRemotingCommand();
        return requestCommand;
    }

    // todo 把CommandDispatcher（CommandTransport）的消息发送功能，整合到这个类中
    //  然后让 MessagingProcessor 依赖 CommandSendingDelegate
    //  需要解决的问题时：怎么初始化和创建 CommandSendingDelegate
}
