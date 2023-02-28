package me.yq.biz.domain;

/**
 * 登录请求实体
 * @author yq
 * @version v1.0 2023-02-15 9:57 AM
 */
public class LoginInfo {
    private final long userId;
    private final String passwd;

    public LoginInfo(long userId, String passwd) {
        this.userId = userId;
        this.passwd = passwd;
    }

    public long getUserId() {
        return userId;
    }

    public String getPasswd() {
        return passwd;
    }
}
