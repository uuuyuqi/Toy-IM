package me.yq.biz;

import io.netty.channel.Channel;
import me.yq.biz.domain.User;

/**
 * 登出请求，注意登出只有请求没有响应
 * @author yq
 * @version v1.0 2023-02-14 5:32 PM
 */
public class SignOutRequest {
    private User user;

    private Channel channel;

    private Reason reason;

    /**
     * 主动地请求下线，一般由客户端下线会主动以这种方式构造
     * @param user 下线的用户信息
     */
    public SignOutRequest(User user) {
        this.user = user;
        this.reason = Reason.ACTIVE;
    }

    /**
     * 被动地请求下线，一般时服务端发现客户端不可用，会以这种方式构造请求
     * @param channel 不可用的客户端 channel
     */
    public SignOutRequest(Channel channel) {
        this.channel = channel;
        this.reason = Reason.PASSIVE;
    }

    public SignOutRequest() {
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public enum Reason {
        ACTIVE,
        PASSIVE
    }
}
