package me.yq.remoting.processor.impl;

import lombok.extern.slf4j.Slf4j;
import me.yq.biz.SignOutRequest;
import me.yq.biz.domain.User;
import me.yq.remoting.transport.deliver.RequestRecord;
import me.yq.remoting.transport.deliver.process.RequestProcessor;
import me.yq.remoting.transport.deliver.process.RequestWrapper;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.service.OnlineUserService;

/**
 * 登出处理器, 处理 {@link SignOutRequest} 对象
 *
 * @author yq
 * @version v1.0 2023-02-14 12:07 PM
 */
@Slf4j
public class SignOutProcessor extends RequestProcessor {

    private final OnlineUserService onlineUserService = OnlineUserService.getInstance();

    private final RequestRecord requestRecord = RequestRecord.getInstance();

    @Override
    public BaseResponse process(RequestWrapper requestWrapper) {

        try {
            BaseRequest request = requestWrapper.getRequest();
            SignOutRequest signOutRequest = (SignOutRequest) request.getAppRequest();
            User user = signOutRequest.getUser();
            onlineUserService.registerOffline(user);

            log.debug("用户{}已经下线",user.getUserId());
        }finally {
            requestRecord.removeAllTask(requestWrapper.getClientChannel(),3000);
        }


        return new BaseResponse("注销成功!");
    }
}