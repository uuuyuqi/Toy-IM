package me.yq.common;

/**
 * 基础请求类
 * @author yq
 * @version v1.0 2023-02-16 10:54 AM
 */
public class BaseRequest extends BaseTransferObject{
    private final byte bizCode;

    public BaseRequest(byte bizCode, Object appRequest) {
        super(appRequest);
        this.bizCode = bizCode;
    }

    public byte getBizCode() {
        return bizCode;
    }

    public Object getAppRequest(){
        return this.getObjToSend();
    }
}
