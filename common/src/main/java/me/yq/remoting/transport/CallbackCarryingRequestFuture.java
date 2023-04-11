package me.yq.remoting.transport;

import me.yq.remoting.command.DefaultResponseCommand;

import java.util.concurrent.ScheduledFuture;

/**
 * 需要进行回调的 RequestFuture 实现，采用非阻塞的方式获取响应结果，同时执行回调函数。
 * 该类型的 future 一般在后台会有一个定时任务，用以检测该异步 future 是否已经超时。
 * @author yq
 * @version v1.0 2023-04-10 22:49
 */
public class CallbackCarryingRequestFuture extends RequestFuture {

    private DefaultResponseCommand responseCommand;

    private final Callback callback;

    private ScheduledFuture<?> scheduledFuture;

    public CallbackCarryingRequestFuture(int messageId, Callback callback) {
        super(messageId);
        this.callback = callback;
    }

    public DefaultResponseCommand acquireResponse(long timeoutMillis) {
        return responseCommand;
    }

    public void putResponse(DefaultResponseCommand responseCommand) {

        if (scheduledFuture != null)
            scheduledFuture.cancel(true);

        this.responseCommand = responseCommand;

        onCallback();
    }

    /**
     * 调用回调函数（如果有的话）
     */
    private void onCallback() {
        if (callback == null)
            return;

        if (this.responseCommand == null)
            callback.onTimeout();
        else if (this.responseCommand.getThrowable() != null)
            callback.onException(this.responseCommand.getThrowable());
        else {
            // 在回调的情况下，需要在这里提前把反序列化做掉
            this.responseCommand.deserialize();
            callback.onResponse(this.responseCommand.getAppResponse());
        }
    }

    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }


}
