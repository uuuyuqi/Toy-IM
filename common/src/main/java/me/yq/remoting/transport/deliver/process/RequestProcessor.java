package me.yq.remoting.transport.deliver.process;

import me.yq.remoting.transport.support.BaseResponse;

/**
 * 请求处理器
 * @author yq
 * @version v1.0 2023-02-21 14:13
 */
public abstract class RequestProcessor {

    /**
     * 处理业务请求
     * @param request 业务请求对象
     */
    abstract public BaseResponse process(RequestWrapper requestWrapper);

}
