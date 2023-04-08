package me.yq.test.processors;

import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.transport.process.RequestProcessor;

import java.util.List;

/**
 * echo 处理器
 * @author yq
 * @version v1.0 2023-04-07 16:56
 */
public class SimpleEchoProcessor extends RequestProcessor {
    public SimpleEchoProcessor() {
        super(true);
    }

    public SimpleEchoProcessor(List<Runnable> preTasks, List<Runnable> postTasks) {
        super(true, preTasks, postTasks);
    }

    @Override
    public BaseResponse doProcess(BaseRequest request) {
        return new BaseResponse(request.getAppRequest());
    }
}
