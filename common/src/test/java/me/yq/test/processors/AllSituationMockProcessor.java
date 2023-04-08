package me.yq.test.processors;

import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.BusinessException;
import me.yq.remoting.transport.process.RequestProcessor;

import java.util.List;

/**
 * 可以根据请求中传来的不同 situationCode 值来模拟不同的业务处理结果的处理器
 *
 * @author yq
 * @version v1.0 2023-04-07 18:22
 */
public class AllSituationMockProcessor extends RequestProcessor {

    public AllSituationMockProcessor() {
        super(true, null, null);
    }

    public AllSituationMockProcessor(List<Runnable> preTasks, List<Runnable> postTasks) {
        super(true, preTasks, postTasks);
    }

    @Override
    public BaseResponse doProcess(BaseRequest request) {
        Integer situationCode = (Integer) request.getAppRequest();
        if (situationCode == 1)
            return new BaseResponse("ok");
        else if (situationCode == 0)
            return new BaseResponse(ResponseStatus.FAILED);
        else if (situationCode == -1)
            throw new BusinessException("业务异常");
        else
            return new BaseResponse(ResponseStatus.OK_NO_NEED_RESPONSE);
    }


}
