package me.yq.remoting.transport.support;

import me.yq.remoting.transport.support.constant.BizCode;

/**
 * 基础请求类
 * @author yq
 * @version v1.0 2023-02-16 10:54 AM
 */
public class BaseRequest extends BaseTransferObject{
    private final BizCode bizCode;

    public BaseRequest(BizCode bizCode, Object appRequest) {
        super(appRequest);
        this.bizCode = bizCode;
    }

    public BizCode getBizCode() {
        return bizCode;
    }

    public Object getAppRequest(){
        return this.getObjToSend();
    }
}
