package me.yq.remoting.transport;

import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.BusinessException;
import me.yq.remoting.command.DefaultResponseCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 调用任务 future 类，用于等待响应
 */
public class RequestFuture {

    private final int messageId;

    private DefaultResponseCommand responseCommand;

    private final CountDownLatch latch = new CountDownLatch(1);

    private Callback callback;

    private ScheduledFuture<?> scheduledFuture;

    public RequestFuture(int messageId) {
        this.messageId = messageId;
    }

    /**
     * 同步获取响应，注意：该方法是阻塞方法
     * @param timeoutMillis 获取响应的超时时间，为 -1 时，表示无限等待；如果超时，会抛出异常
     * @return 业务响应
     */
    public DefaultResponseCommand waitAndGetResponse(long timeoutMillis) {
        // 在超时时间内做等待
        try {
            if (timeoutMillis == -1)
                latch.await();
            else {
                boolean ok = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                if (!ok)
                    throw new BusinessException("等待响应超时！");
            }
        } catch (InterruptedException ignored) {
        }

        return responseCommand;
    }

    public DefaultResponseCommand getResponseCommand() {
        return responseCommand;
    }

    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void putFailedResponse(Throwable t) {
        putResponse(createFailedCommand(t));
    }


    public void putResponse(DefaultResponseCommand responseCommand) {
        // 第一件事就是取消超时任务
        if (scheduledFuture != null)
            scheduledFuture.cancel(true);

        this.responseCommand = responseCommand;
        this.latch.countDown(); // 保证请求处结束阻塞

    }

    public int getMessageId() {
        return messageId;
    }



    /**
     * 构造一个失败的响应，通常发生在内部响应失败的场景下，直接返回一个失败。<br/>
     *
     * @param t     异常
     * @return 失败的响应，包裹了具体的异常
     */
    private DefaultResponseCommand createFailedCommand(Throwable t) {
        BaseResponse response = new BaseResponse(t);
        response.setStatus(ResponseStatus.FAILED);
        response.setReturnMsg("获取响应失败！ 错误信息: " + t.getMessage());
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(messageId);
        responseCommand.setAppResponse(response);
        return responseCommand;
    }

    private DefaultResponseCommand createFailedCommand(int msgId, String errorMessage) {
        BaseResponse response = new BaseResponse(null);
        response.setStatus(ResponseStatus.FAILED);
        response.setReturnMsg("获取响应失败！ 错误信息: " + errorMessage);
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(msgId);
        responseCommand.setAppResponse(response);
        return responseCommand;
    }
}
