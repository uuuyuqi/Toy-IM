package me.yq.common;

import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.transport.RequestFuture;

/**
 * 基础响应类的 future，可以通过该类对象获取 future 响应结果，用于异步调用的场景
 * @author yq
 * @version v1.0 2023-04-10 21:22
 */
public class BaseResponseFuture {

    private final RequestFuture requestFuture;

    private BaseResponse response;

    public BaseResponseFuture(RequestFuture requestFuture) {
        this.requestFuture = requestFuture;
    }

    /**
     * 获取响应结果，如果超时，则返回 null
     * @param timeoutMillis 超时时间
     * @return 响应结果
     */
    public BaseResponse get(long timeoutMillis){
        DefaultResponseCommand responseCommand = requestFuture.acquireAndClose(timeoutMillis);
        responseCommand.deserialize();
        return responseCommand.getAppResponse();
    }

    public BaseResponse getResponse() {
        return response;
    }

    public void setResponse(BaseResponse response) {
        this.response = response;
    }
}
