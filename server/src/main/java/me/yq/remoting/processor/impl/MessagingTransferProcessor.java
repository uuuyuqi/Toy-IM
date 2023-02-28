package me.yq.remoting.processor.impl;

import me.yq.biz.Message;
import me.yq.biz.domain.User;
import me.yq.service.OnlineUserService;
import me.yq.support.ServerBootstrap;
import me.yq.remoting.transport.deliver.CommandSendingDelegate;
import me.yq.remoting.transport.deliver.process.RequestProcessor;
import me.yq.remoting.transport.deliver.process.RequestWrapper;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.BizCode;
import me.yq.remoting.transport.support.constant.ResponseStatus;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


/**
 * 消息收发处理器, 处理 {@link Message} 对象, Messaging 的处理非常特殊，需要进行二次转发！
 *
 * @author yq
 * @version v1.0 2023-02-14 12:07 PM
 */
@Slf4j
public class MessagingTransferProcessor extends RequestProcessor {

    private final OnlineUserService onlineUserService = OnlineUserService.getInstance();

    private final ServerBootstrap serverBootstrap;

    public MessagingTransferProcessor(ServerBootstrap serverBootstrap) {
        this.serverBootstrap = serverBootstrap;
    }

    /**
     * 接收 fromUser 的业务消息，并将消息发送给 targetUser
     * @param requestWrapper 消息包装，包装了消息的来源 channel
     * @return 消息发送结果（消息是否发送成功）
     */
    @Override
    public BaseResponse process(RequestWrapper requestWrapper) {
        BaseRequest request = requestWrapper.getRequest();
        Message message = (Message) request.getAppRequest();
        User targetUser = message.getToUser();

        boolean online = onlineUserService.checkOnlineState(targetUser);
        if (!online) {
            throw new RuntimeException("对方用户不在线!");
        }

        BaseResponse response;
        try {
            response = sendMessageToTarget(message, targetUser);
        }catch (Exception e) {
            log.error("向对方发送消息时出现异常，信息：{}",e.getMessage());
            response = new BaseResponse(ResponseStatus.FAILED,"出现了一些异常，要不重发试试？",null);
        }
        return response;
    }

    /**
     * 将消息转发送给目标用户
     * @param message 待发送的消息
     * @param targetUser 目标用户
     * @return 发送结果
     */
    private BaseResponse sendMessageToTarget(Message message, User targetUser){
        Channel targetChannel = onlineUserService.getUserChannel(targetUser);

        CommandSendingDelegate sendingDelegate = this.serverBootstrap.getSendingDelegate();
        BaseRequest request = new BaseRequest(BizCode.Messaging,message);

        return sendingDelegate.sendRequestSync(targetChannel,request);
    }
}
