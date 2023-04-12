package me.yq.remoting.transport;

import me.yq.common.BaseResponse;
import me.yq.remoting.command.DefaultResponseCommand;

import java.lang.ref.WeakReference;

/**
 * 调用任务 future 类，用于等待响应
 */
public abstract class RequestFuture {

    private final int messageId;

    /**
     * 该 future 属于哪个 RequestFutureMap，可以直接通过 future 来进行 remove 操作，避免了额外的 map 操作
     */
    private final WeakReference<RequestFutureMap> belongsTo;

    public RequestFuture(int messageId, RequestFutureMap belongsTo) {
        this.messageId = messageId;
        this.belongsTo = new WeakReference<>(belongsTo);
    }

    /**
     * 获取响应，不同的子类实现会有不同获取响应的方法
     *
     * @param timeoutMillis 获取响应的超时时间
     * @return DefaultResponseCommand 通信层响应对象
     */
    public abstract DefaultResponseCommand acquireResponse(long timeoutMillis);

    public void putFailedResponse(Throwable t) {
        putResponse(createFailedCommand(t));
    }

    public abstract void putResponse(DefaultResponseCommand responseCommand);

    public int getMessageId() {
        return messageId;
    }

    /**
     * 关闭 future，主要是将 future 自己从 RequestFutureMap 中移除。
     * 因为到这个阶段，future 已经不会再被使用了，应该进行移除，否则会造成内存泄漏。
     */
    public void close() {
        try {
            RequestFutureMap requestFutureMap = belongsTo.get();
            if (requestFutureMap != null) {
                requestFutureMap.removeSuchFuture(messageId);
            }
        } catch (NullPointerException ignored) {
            // npe? whatever!
        }
    }


    /**
     * 构造一个失败的响应，通常发生在内部响应失败的场景下，直接返回一个失败。<br/>
     *
     * @param t 异常
     * @return 失败的响应，包裹了具体的异常
     */
    private DefaultResponseCommand createFailedCommand(Throwable t) {
        BaseResponse response = new BaseResponse(t);
        response.setReturnMsg("获取响应失败！ 错误信息: " + t.getMessage());
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(messageId);
        responseCommand.setAppResponse(response);
        return responseCommand;
    }
}
