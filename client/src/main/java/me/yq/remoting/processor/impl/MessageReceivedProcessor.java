package me.yq.remoting.processor.impl;

import me.yq.biz.Message;
import me.yq.support.ChatClient;
import me.yq.remoting.transport.deliver.process.RequestProcessor;
import me.yq.remoting.transport.deliver.process.RequestWrapper;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.ResponseStatus;

/**
 * 消息接收处理器
 * @author yq
 * @version v1.0 2023-02-28 11:33
 */
public class MessageReceivedProcessor extends RequestProcessor {

    private final ChatClient client;

    public MessageReceivedProcessor(ChatClient client) {
        this.client = client;
    }

    @Override
    public BaseResponse process(RequestWrapper requestWrapper) {
        try {
            BaseRequest request = requestWrapper.getRequest();
            Message message = (Message) request.getAppRequest();
            client.acceptMsg(message.getFromUser(),message.getMsg());
        }catch (Exception e) {
            return new BaseResponse(ResponseStatus.FAILED,"接收方出了一点意外，请稍后再试～",null);
        }

        return new BaseResponse("接收方已收到信息！");
    }
}
