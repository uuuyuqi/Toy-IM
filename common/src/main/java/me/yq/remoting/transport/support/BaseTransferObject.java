package me.yq.remoting.transport.support;

/**
 * 基础传输对象
 * @author yq
 * @version v1.0 2023-02-16 10:56 AM
 */
public abstract class BaseTransferObject {

    private final Object objToSend;

    /**
     * 自定义一个传输对象，一般用于构造请求
     * @param objToSend 待传送的对象
     */
    public BaseTransferObject(Object objToSend) {
        this.objToSend = objToSend;
    }



    protected Object getObjToSend() {
        return objToSend;
    }
}
