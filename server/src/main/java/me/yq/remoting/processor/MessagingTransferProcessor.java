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
import me.yq.remoting.config.Config;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.transport.process.RequestProcessor;


/**
 * 消息收发处理器, 处理 {@link Message} 对象, Messaging 的处理非常特殊，需要进行二次转发！
 *
 * @author yq
 * @version v1.0 2023-02-14 12:07 PM
 */
@Slf4j
public class MessagingTransferProcessor extends RequestProcessor {

    private final ServerSessionMap serverSessionMap;

    private final Config config;


    public MessagingTransferProcessor(ServerSessionMap serverSessionMap, Config config) {
        this.serverSessionMap = serverSessionMap;
        this.config = config;
    }

    /**
     * 接收 fromUser 的业务消息，并将消息发送给 targetUser
     * @param request 业务消息请求
     * @return 消息发送结果（消息是否发送成功）
     */
    @Override
    public BaseResponse process(BaseRequest request) {
        Message message = (Message) request.getAppRequest();
        User targetUser = message.getToUser();

        boolean online = serverSessionMap.checkExists(targetUser.getUserId());
        if (!online) {
            throw new BusinessException("对方用户不在线!");
        }

        BaseResponse response;
        try {
            response = sendMessageToTarget(message, targetUser, config.getLong(ServerConfigNames.WAIT_RESPONSE_MILLIS));
            if (response.getStatus() != ResponseStatus.SUCCESS)
                throw new SystemException("向目标用户发送信息失败！信息：" + response.getReturnMsg(),(Throwable) response.getAppResponse());
        }catch (Exception e) {
            log.error("向对方发送消息时出现异常，信息：{}",e.getMessage());
            response = new BaseResponse(ResponseStatus.FAILED,"对方网络不佳，重发消息试试？",null);
        }
        return response;
    }

    /**
     * 将消息转发送给目标用户
     * @param message 待发送的消息
     * @param targetUser 目标用户
     * @return 发送结果
     */
    private BaseResponse sendMessageToTarget(Message message, User targetUser, long timeoutMillis) {
        Channel targetChannel = serverSessionMap.getUserChannel(targetUser.getUserId());
        BaseRequest request = new BaseRequest(BizCode.Messaging,message);
        return CommandSendingDelegate.sendRequestSync(targetChannel,request,timeoutMillis);
    }
}
