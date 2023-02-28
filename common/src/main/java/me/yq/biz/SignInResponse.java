package me.yq.biz;

/**
 * 登录响应
 * @author yq
 * @version v1.0 2023-02-14 5:31 PM
 */
public class SignInResponse {
    /**
     * 登录是否成功
     */
    private boolean ok;

    /**
     * 如果失败，附加原因
     */
    private String cause;

    public SignInResponse() {
    }

    public SignInResponse(boolean ok, String cause) {
        this.ok = ok;
        this.cause = cause;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public boolean isOk() {
        return ok;
    }

    public String getCause() {
        return cause;
    }
}
