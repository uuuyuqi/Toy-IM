package me.yq.remoting.transport;

import me.yq.common.exception.BusinessException;
import me.yq.remoting.command.DefaultResponseCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 默认的 RequestFuture 实现，采用阻塞方式来获取响应结果
 * @author yq
 * @version v1.0 2023-04-10 22:47
 */
public class DefaultRequestFuture extends RequestFuture {

    private DefaultResponseCommand responseCommand;

    private final CountDownLatch latch = new CountDownLatch(1);

    public DefaultRequestFuture(int messageId, RequestFutureMap requestFutureMap) {
        super(messageId,requestFutureMap);
    }

    /**
     * 同步获取响应，注意：该方法是阻塞方法
     *
     * @param timeoutMillis 获取响应的超时时间，为 -1 时，表示无限等待；如果超时，会抛出异常
     * @return 业务响应
     */
    protected DefaultResponseCommand acquireResponse(long timeoutMillis) {
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


    public void putResponse(DefaultResponseCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.latch.countDown(); // 保证请求处结束阻塞
    }
}
