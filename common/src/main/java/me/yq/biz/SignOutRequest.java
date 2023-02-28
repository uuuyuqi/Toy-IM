package me.yq.biz;

import me.yq.biz.domain.User;

/**
 * 登出请求，注意登出只有请求没有响应
 * @author yq
 * @version v1.0 2023-02-14 5:32 PM
 */
public class SignOutRequest {
    private User user;

    public SignOutRequest(User user) {
        this.user = user;
    }

    public SignOutRequest() {
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
