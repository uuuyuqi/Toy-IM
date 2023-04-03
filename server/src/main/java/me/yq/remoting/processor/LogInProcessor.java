package me.yq.remoting.processor;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import me.yq.biz.LogInRequest;
import me.yq.biz.domain.User;
import me.yq.biz.service.LoginService;
import me.yq.biz.service.SendNoticeService;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.remoting.config.Config;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.support.session.Session;
import me.yq.remoting.transport.process.RequestProcessor;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 登录处理器, 处理 {@link LogInRequest} 对象
 *
 * @author yq
 * @version v1.0 2023-02-14 12:06 PM
 */
@Slf4j
public class LogInProcessor extends RequestProcessor {

    // 业务服务
    private final LoginService loginService = LoginService.getInstance();
    private final SendNoticeService sendNoticeService = SendNoticeService.getInstance();

    private final ServerSessionMap serverSessionMap;

    private final Config config;

    public LogInProcessor(ServerSessionMap serverSessionMap, Config config) {
        this.serverSessionMap = serverSessionMap;
        this.config = config;
    }

    public LogInProcessor(ServerSessionMap serverSessionMap, Config config, List<Runnable> preTasks, List<Runnable> postTasks) {
        super(preTasks, postTasks);
        this.serverSessionMap = serverSessionMap;
        this.config = config;

    }


    @Override
    public BaseResponse doProcess(BaseRequest request){

        Channel channel = getChannelLocal().get();

        LogInRequest logInRequest = (LogInRequest) request.getAppRequest();
        User user = logInRequest.getUser();

        // 校验登录用户的用户名密码
        User userFound = loginService.login(user);

        // 先判断用户是否已经登录
        // - 已经登录，就会将之前的登录挤掉，给客户端推送一个警告信息，并关闭老 channel
        // - 没有登录，将对象添加到 session
        Session oldSession = serverSessionMap.addSession(new Session(user.getUserId(),channel));

        if (oldSession != null){
            sendNoticeService.sendNotice(
                    "[下线警告]",
                    "检测到您的账号在另一处登录 ip [" + ((InetSocketAddress)channel.remoteAddress()).getAddress().getHostAddress() + "]，如非本人操作，请立即修改密码！",
                    oldSession,
                    config.getLong(ServerConfigNames.WAIT_RESPONSE_MILLIS));
            // 老 channel 会被强行 close
            log.warn("发生挤掉线行为，现在强行关闭老 channel！");
            oldSession.getChannel().close();
        }

        log.debug("用户[{}]信息校验通过！登陆成功！",user.getUserId());
        return new BaseResponse(ResponseStatus.SUCCESS,"登录成功!",userFound);
    }

}




