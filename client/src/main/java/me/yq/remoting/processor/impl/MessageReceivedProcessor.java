package me.yq.remoting.processor.impl;

import me.yq.biz.Message;
import me.yq.remoting.transport.deliver.process.RequestProcessor;
import me.yq.remoting.transport.deliver.process.RequestWrapper;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.ResponseStatus;
import me.yq.support.ChatClient;

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

        BaseRequest request = requestWrapper.getRequest();
        Message message = (Message) request.getAppRequest();
        try {
            client.acceptMsg(message.getFromUser(),message.getMsg());
        }catch (Exception e) {
            e.printStackTrace();
            return new BaseResponse(ResponseStatus.FAILED,"接收方出了一点意外，请稍后再试～",e);
        }

        return new BaseResponse("[" + message.getToUser() +"]已收到信息！");
    }
}
