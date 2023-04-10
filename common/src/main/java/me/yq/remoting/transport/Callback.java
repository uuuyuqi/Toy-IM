package me.yq.remoting.transport;

import me.yq.common.BaseResponse;

/**
 * 回调接口，异步调用结束之后，可以通过 callback 的形式进行进行下一步操作
 * @author yq
 * @version v1.0 2023-04-09 13:37
 */
public interface Callback {

    /**
     * 异步调用结束之后，会调用该方法
     * @param response 响应数据
     */
    void onResponse(BaseResponse response);

    /**
     * 异步调用出现异常时，会调用该方法
     * @param cause 异常信息
     */
    void onException(Throwable cause);

    /**
     * 异步调用超时时，会调用该方法
     */
    void onTimeout();
}
