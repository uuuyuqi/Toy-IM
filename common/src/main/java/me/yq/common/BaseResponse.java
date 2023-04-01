package me.yq.common;

import java.util.Objects;

/**
 * 基础响应类
 * @author yq
 * @version v1.0 2023-02-16 10:55 AM
 */
public class BaseResponse extends BaseTransferObject{
    /**
     * 返回状态
     */
    private ResponseStatus status;

    /**
     * 返回信息
     */
    private String returnMsg;

    public BaseResponse(ResponseStatus responseStatus){
        super(null);
        this.status = responseStatus;
    }

    /**
     * 出现异常的情况下，appResponse 是一个 Throwable 对象
     * @param appResponse  响应对象
     */
    public BaseResponse(Object appResponse) {
        super(appResponse);
        if (appResponse instanceof Throwable){
            this.status = ResponseStatus.FAILED;
            this.returnMsg = ((Throwable) appResponse).getMessage();
        }
        else {
            this.status = ResponseStatus.SUCCESS;
            this.returnMsg = "success";
        }

    }

    public BaseResponse(ResponseStatus status, String errorMsg, Object appResponse) {
        super(appResponse);
        this.status = status;
        this.returnMsg = errorMsg;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(byte statusCode) {
        this.status = Objects.requireNonNull(ResponseStatus.lookup(statusCode));
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public String getReturnMsg() {
        return returnMsg;
    }

    public void setReturnMsg(String returnMsg) {
        this.returnMsg = returnMsg;
    }

    public Object getAppResponse(){
        return this.getObjToSend();
    }
}
