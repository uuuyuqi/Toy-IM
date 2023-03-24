package me.yq.biz;

import me.yq.biz.domain.User;

/**
 * 登出请求，注意登出只有请求没有响应
 * @author yq
 * @version v1.0 2023-02-14 5:32 PM
 */
public class LogOutRequest {
    private User user;

    /**
     * 主动地请求下线，一般由客户端下线会主动以这种方式构造
     * @param user 下线的用户信息
     */
    public LogOutRequest(User user) {
        this.user = user;
    }

    public LogOutRequest() {
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

}
