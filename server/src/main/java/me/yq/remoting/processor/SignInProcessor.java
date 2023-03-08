package me.yq.remoting.processor;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import me.yq.biz.SignInRequest;
import me.yq.biz.domain.User;
import me.yq.remoting.transport.deliver.process.RequestProcessor;
import me.yq.remoting.transport.deliver.process.RequestWrapper;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.ResponseStatus;
import me.yq.service.OnlineUserService;
import me.yq.service.ValidateLoginService;

/**
 * 登录处理器, 处理 {@link SignInRequest} 对象
 *
 * @author yq
 * @version v1.0 2023-02-14 12:06 PM
 */
@Slf4j
public class SignInProcessor extends RequestProcessor {

    // 业务服务
    private final ValidateLoginService validateLoginService = new ValidateLoginService();
    private final OnlineUserService onlineUserService = OnlineUserService.getInstance();

    @Override
    public BaseResponse process(RequestWrapper requestWrapper){

        BaseRequest request = requestWrapper.getRequest();
        Channel channel = requestWrapper.getClientChannel();

        SignInRequest signInRequest = (SignInRequest) request.getAppRequest();

        // 校验登录用户的用户名密码
        User user = validateLoginService.checkLoginInfo(signInRequest.getUser());

        // 将对象添加到已上线列表
        onlineUserService.registerOnline(user, channel);

        log.debug("用户[{}]信息校验通过！登陆成功！",user.getUserId());
        return new BaseResponse(ResponseStatus.SUCCESS,"登录成功!",user);
    }

}




