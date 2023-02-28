package me.yq.biz;

import me.yq.biz.domain.User;

/**
 * 登录请求
 * @author yq
 * @version v1.0 2023-02-14 5:31 PM
 */
public class SignInRequest {
    private User user;

    public SignInRequest() {
    }

    public SignInRequest(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
