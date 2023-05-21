package me.yq.remoting.processor;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import me.yq.biz.Message;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.BizCode;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.BusinessException;
import me.yq.common.exception.SystemException;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.Config;
import me.yq.remoting.transport.Callback;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.transport.process.RequestProcessor;

import java.util.List;
import java.util.concurrent.Executor;


/**
 * 消息收发处理器, 处理 {@link Message} 对象, Messaging 的处理非常特殊，需要进行二次转发！
 *
 * @author yq
 * @version v1.0 2023-02-14 12:07 PM
 */
@Slf4j
public class MessagingTransferProcessor extends RequestProcessor {

    private final SessionMap sessionMap;

    private final Config config;

    private final Executor executor;


    public MessagingTransferProcessor(SessionMap sessionMap, Config config, Executor executor) {
        super(true);
        this.sessionMap = sessionMap;
        this.config = config;
        this.executor = executor;
    }

    public MessagingTransferProcessor(SessionMap sessionMap, Config config, Executor executor, List<Runnable> preTasks, List<Runnable> postTasks) {
        super(true, preTasks, postTasks);
        this.sessionMap = sessionMap;
        this.config = config;
        this.executor = executor;
    }

    /**
     * 接收 fromUser 的业务消息，并将消息发送给 targetUser
     *
     * @param request 业务消息请求
     * @return 消息发送结果（消息是否发送成功）
     */
    @Override
    public BaseResponse doProcess(BaseRequest request) {
        Message message = (Message) request.getAppRequest();
        User targetUser = message.getToUser();

        boolean online = sessionMap.checkExists(targetUser.getUserId());
        if (!online) {
            throw new BusinessException("对方用户不在线!");
        }

        sendMessageToTarget(message, targetUser, config.getLong(ServerConfigNames.WAIT_RESPONSE_MILLIS));
        return new BaseResponse(ResponseStatus.SUCCESS);
    }

    /**
     * 将消息转发送给目标用户，异步告知用户是否发送成功
     *
     * @param message    待发送的消息
     * @param targetUser 目标用户
     */
    private void sendMessageToTarget(Message message, User targetUser, long timeoutMillis) {
        Channel targetChannel = sessionMap.getUserChannel(targetUser.getUserId());
        BaseRequest request = new BaseRequest(BizCode.Messaging.code(), message);
        CommandSendingDelegate.sendRequestCallback(
                targetChannel,
                request,
                timeoutMillis,
                new MessageSendFailedCallback(getChannelLocal().get(), message.getMessageId(),this.executor));
    }

    /**
     * 消息发送失败回调，需要通知发送端这个消息发送失败了
     */
    private static class MessageSendFailedCallback implements Callback {

        private final Channel fromChannel;

        private final int messageId;

        private final Executor executor;

        public MessageSendFailedCallback(Channel fromChannel, int messageId, Executor executor) {
            this.fromChannel = fromChannel;
            this.messageId = messageId;
            this.executor = executor;
        }

        @Override
        public void onResponse(BaseResponse response) {
            CommandSendingDelegate.sendResponseOneway(fromChannel.pipeline().lastContext(), messageId, new BaseResponse(response.getAppResponse()));
        }

        @Override
        public void onException(Throwable cause) {
            CommandSendingDelegate.sendResponseOneway(fromChannel.pipeline().lastContext(), messageId, new BaseResponse(new SystemException("不好意思服务器开小差了，请稍后再试！")));
        }

        @Override
        public void onTimeout() {
            CommandSendingDelegate.sendResponseOneway(fromChannel.pipeline().lastContext(), messageId, new BaseResponse(new SystemException("不好意思服务器开小差了，请稍后再试！")));
        }

        @Override
        public Executor getExecutor() {
            return this.executor;
        }
    }
}
