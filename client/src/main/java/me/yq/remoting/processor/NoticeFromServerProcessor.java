package me.yq.remoting.processor;

import me.yq.biz.Notice;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.transport.process.RequestProcessor;
import me.yq.support.ChatClient;

import java.util.List;

/**
 * 专门处理来自服务端的 notice 通知
 * @author yq
 * @version v1.0 2023-03-16 17:42
 */
public class NoticeFromServerProcessor  extends RequestProcessor {

    private final ChatClient client;

    public NoticeFromServerProcessor(ChatClient client) {
        super(true);
        this.client = client;
    }

    public NoticeFromServerProcessor(ChatClient client,List<Runnable> preTasks,List<Runnable> postTasks) {
        super(true,preTasks,postTasks);
        this.client = client;
    }

    @Override
    public BaseResponse doProcess(BaseRequest request) {
        Notice notice = (Notice) request.getAppRequest();
        if (notice != null)
            client.acceptNotice(notice);
        return new BaseResponse(client.getCurrentUser().getUserId() + " 已收到通知");
    }
}
