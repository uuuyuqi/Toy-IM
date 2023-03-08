package me.yq.remoting.processor;

import io.netty.channel.Channel;
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

            // 客户主动下线
            if (signOutRequest.getReason() == SignOutRequest.Reason.ACTIVE) {
                User user = signOutRequest.getUser();
                onlineUserService.registerOffline(user);
                log.info("用户{}已经下线",user.getUserId());
            }
            // 因为网络等问题导致平台发现用户已事实下线
            else if (signOutRequest.getReason() == SignOutRequest.Reason.PASSIVE){
                Channel channel = signOutRequest.getChannel();
                onlineUserService.removeDisconnectUser(channel);
                log.info("");
            }
        }finally {
            requestRecord.removeAllTaskFromChannel(requestWrapper.getClientChannel(),3000);
        }

        return new BaseResponse("注销成功!");
    }
}