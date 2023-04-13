package me.yq.remoting.transport;

import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.transport.process.CommandHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 请求缓存器，主要功能有 2<br/>
 * - 请求处理器会在此进行 countdown 等待，响应处理器会告知哪个请求已经等待完毕<br/>
 * - 会在此记录请求的 id，以实现线上乱序传输！提升效率！<br/>
 * 该类的设计，参考了 sofa-bolt 中的 invokeFuture。 该类对象将会是 "channel local" 类型，也就是说，
 * 每个 channel 都应该会有请求记录器，进行请求的记录和响应的等待。
 *
 * @author yq
 * @version v1.0 2023-02-21 15:08
 */
@Slf4j
public final class RequestFutureMap {

    private final Map<Integer, RequestFuture> futureMap = new ConcurrentHashMap<>();

    /**
     * 记录发出的请求，以保证收到响应时，能发给对应的业务处理处。<br/>
     *
     * @param future 请求 future
     */
    public void addNewFuture(RequestFuture future) {
        if (futureMap.containsKey(future.getMessageId()))
            throw new RuntimeException("重复的请求 id：" + future.getMessageId());
        futureMap.put(future.getMessageId(),future);
    }

    /**
     * io 线程收到响应后，会提交到 context 中， 供请求侧获取<br/>
     * 参考：{@link CommandHandler#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)}
     */
    public void commitResponseCommand(DefaultResponseCommand responseCommand) {
        int msgId = responseCommand.getMessageId();
        RequestFuture requestFuture = futureMap.get(msgId);

        // == null 表示该响应早已失效（可能是超时）
        if (requestFuture == null)
            return;

        requestFuture.putResponse(responseCommand);
    }

    /**
     * 移除调用任务
     *
     * @param msgId 消息 id
     */
    public void removeSuchFuture(int msgId) {
        this.futureMap.remove(msgId);
    }

    /**
     * 优雅关闭！
     * 相对优雅地移除所有的任务
     *
     * @param timeoutMillis 关闭时应该等待的过期时间
     */
    public synchronized void removeAllFuturesSafe(long timeoutMillis) {
        if (timeoutMillis < 0) throw new IllegalArgumentException("非法的过期时间: " + timeoutMillis);

        long time = System.currentTimeMillis();

        while (System.currentTimeMillis() - time > timeoutMillis) {
            if (!hasRequestFuture()) {
                log.debug("当前已发起的任务均在限定时间内执行完毕!");
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                log.warn("现在准备强制关闭所有任务！");
                break;
            }
        }
        unsafeRemoveAll();
    }

    /**
     * 不安全地强行删除所有任务，此时对于发出去的请求，其结果会被 io 线程丢弃。参考： {@link CommandHandler#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)}
     */
    private void unsafeRemoveAll() {
        if (hasRequestFuture())
            log.warn("当前仍存有未执行完的任务，现在已准备强行清除!");
        this.futureMap.clear();
    }


    /**
     * 判断当前是否仍有请求待发送或未处理完，该方法主要用于优雅停机
     *
     * @return 如果当前有正在处理的请求，则返回 true
     */
    public boolean hasRequestFuture() {
        return futureMap.size() > 0;
    }


}
