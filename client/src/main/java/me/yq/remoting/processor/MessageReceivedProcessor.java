package me.yq.remoting.processor;

import me.yq.biz.Message;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.remoting.transport.process.RequestProcessor;
import me.yq.support.ChatClient;

import java.util.List;

/**
 * 消息接收处理器
 * @author yq
 * @version v1.0 2023-02-28 11:33
 */
public class MessageReceivedProcessor extends RequestProcessor {

    /**
     * 终端，用于展示消息，和客户度操作
     */
    private final ChatClient client;

    public MessageReceivedProcessor(ChatClient client) {
        this.client = client;
    }

    public MessageReceivedProcessor(ChatClient client,List<Runnable> preTasks,List<Runnable> postTasks) {
        super(preTasks,postTasks);
        this.client = client;
    }

    @Override
    public BaseResponse doProcess(BaseRequest request) {

        Message message = (Message) request.getAppRequest();
        try {
            client.acceptMsg(message.getFromUser(),message.getMsg());
        }catch (Exception e) {
            e.printStackTrace();
            return new BaseResponse(ResponseStatus.FAILED,"对方网络不佳，请稍后再试～",e);
        }

        return new BaseResponse("[" + message.getToUser() +"]已收到信息！");
    }
}
