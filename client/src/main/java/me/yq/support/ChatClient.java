package me.yq.support;

import me.yq.biz.SignInRequest;
import me.yq.biz.SignOutRequest;
import me.yq.biz.domain.User;
import me.yq.remoting.RemotingClient;
import me.yq.support.ClientBootstrap;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.BizCode;
import me.yq.remoting.transport.support.constant.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天客户端
 *
 * @author yq
 * @version v1.0 2023-02-12 10:00
 */
@Slf4j
public class ChatClient {

    /**
     * 如果支持多用户登录，这里的 User 需要改成 List<User>
     */
    private User currentUser;

    /**
     * 通信客户端
     */
    private final RemotingClient remotingClient;


    private ClientStatus currentStatus;

    ChatClient(RemotingClient remotingClient) {
        this.remotingClient = remotingClient;
        this.currentStatus = ClientStatus.INITIATED;
    }


    public synchronized void start() {
        try {
            if (currentStatus != ClientStatus.INITIATED) {
                if (currentStatus == ClientStatus.RUNNING) {
                    log.info("服务器正在运行，无需启动！");
                    return;
                }
                this.currentStatus = ClientStatus.EXCEPTION_CLOSED;
                throw new RuntimeException("服务器状态异常！请联系开发人员......");
            }

            this.remotingClient.start();
            this.currentStatus = ClientStatus.RUNNING;
        }catch (Exception e) {
            this.currentStatus = ClientStatus.EXCEPTION_CLOSED;
            throw new RuntimeException("启动失败！错误原因: " + e.getMessage());
        }

    }


    //======================================================
    // biz method
    //======================================================

    /**
     * 登录
     *
     * @param userId 用户账号
     * @param passwd 用户密码
     */
    public void signIn(long userId, String passwd) {
        SignInRequest signInRequest = new SignInRequest();
        signInRequest.setUser(new User(userId, passwd));

        BaseRequest request = new BaseRequest(BizCode.SignInRequest, signInRequest);
        BaseResponse response = remotingClient.sendRequest(request);
        if (response.getStatus() == ResponseStatus.SUCCESS)
            log.info("登陆成功！服务端信息：{}",response.getReturnMsg());
        else {
            throw new RuntimeException("登陆失败！原因: " + response.getReturnMsg());
        }
    }

    /**
     * 登出
     *
     * @param userId 用户账号
     */
    public void signOut(long userId) {
        SignOutRequest signOutRequest = new SignOutRequest();
        signOutRequest.setUser(new User(userId));

        BaseRequest request = new BaseRequest(BizCode.SignOutRequest,signOutRequest);
        BaseResponse response = remotingClient.sendRequest(request);
        if (response.getStatus() == ResponseStatus.SUCCESS)
            log.info("下线成功！服务端信息：{}",response.getReturnMsg());
        else {
            throw new RuntimeException("下线失败！原因: " + response.getReturnMsg());
        }
    }

    /**
     * 向好友发送信息
     *
     * @param targetUserId 好友 id
     * @param msg          消息内容
     */
    public void sendMsg(long targetUserId, String msg) {
        // todo
    }


    /**
     * 接收来自好友的消息
     *
     * @param fromUserId 好友 id
     * @param msg        消息内容
     */
    public void acceptMsg(User user, String msg) {
        log.info("收到信息来自[{}]的信息：{}",user.getName(),msg);
    }


    enum ClientStatus {
        INITIATED,
        RUNNING,
        CLOSED,
        EXCEPTION_CLOSED
    }

}
