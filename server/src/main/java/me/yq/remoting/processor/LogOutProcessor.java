package me.yq.remoting.processor;

import lombok.extern.slf4j.Slf4j;
import me.yq.biz.LogOutRequest;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.transport.process.RequestProcessor;

import java.util.List;

/**
 * 登出处理器, 处理 {@link LogOutRequest} 对象
 *
 * @author yq
 * @version v1.0 2023-02-14 12:07 PM
 */
@Slf4j
public class LogOutProcessor extends RequestProcessor {

    private final SessionMap sessionMap = SessionMap.getInstanceOrCreate(null);

    public LogOutProcessor() {
        super(false);
    }

    public LogOutProcessor(List<Runnable> preTasks,List<Runnable> postTasks) {
        super(false,preTasks,postTasks);
    }


    @Override
    public BaseResponse doProcess(BaseRequest request) {

        LogOutRequest logOutRequest = (LogOutRequest) request.getAppRequest();
        User user = logOutRequest.getUser();

        sessionMap.removeSessionSafe(user.getUserId());
        log.info("用户{}已经下线", user.getUserId());

        // 关闭 channel
        getChannelLocal().get().close();

        return new BaseResponse(ResponseStatus.OK_NO_NEED_RESPONSE);
    }
}