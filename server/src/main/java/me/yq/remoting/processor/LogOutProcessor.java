package me.yq.remoting.processor;

import lombok.extern.slf4j.Slf4j;
import me.yq.biz.LogOutRequest;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.transport.process.RequestProcessor;

/**
 * 登出处理器, 处理 {@link LogOutRequest} 对象
 *
 * @author yq
 * @version v1.0 2023-02-14 12:07 PM
 */
@Slf4j
public class LogOutProcessor extends RequestProcessor {

    private final ServerSessionMap serverSessionMap = ServerSessionMap.getInstanceOrCreate(null);

    @Override
    public BaseResponse process(BaseRequest request) {

        LogOutRequest logOutRequest = (LogOutRequest) request.getAppRequest();
        User user = logOutRequest.getUser();

        serverSessionMap.removeSessionSafe(user.getUserId());
        log.info("用户{}已经下线", user.getUserId());

        return new BaseResponse("注销成功!");
    }
}