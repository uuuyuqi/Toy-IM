package me.yq.remoting.processor;

import me.yq.biz.Notice;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.transport.process.RequestProcessor;
import me.yq.support.ChatClient;

/**
 * 专门处理来自服务端的 notice 通知
 * @author yq
 * @version v1.0 2023-03-16 17:42
 */
public class NoticeFromServerProcessor  extends RequestProcessor {

    private final ChatClient client;

    public NoticeFromServerProcessor(ChatClient client) {
        this.client = client;
    }

    @Override
    public BaseResponse process(BaseRequest request) {
        Notice notice = (Notice) request.getAppRequest();
        if (notice != null)
            client.acceptNotice(notice);
        return new BaseResponse(client.getCurrentUser().getUserId() + " 已收到通知");
    }
}
