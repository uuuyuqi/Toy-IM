package me.yq.remoting.transport.deliver;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.transport.command.DefaultResponseCommand;
import me.yq.remoting.transport.command.RemotingCommand;
import me.yq.remoting.transport.constant.DefaultConfig;
import me.yq.remoting.transport.deliver.process.CommandHandler;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.ResponseStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 请求记录器，主要功能有 2<br/>
 * - 请求处理器会在此进行 countdown 等待，响应处理器会告知哪个请求已经等待完毕<br/>
 * - 会在此记录请求的 id，以实现线上乱序传输！提升效率！<br/>
 * 该类的设计，参考了 sofa-bolt 中的 invokeFuture
 * @author yq
 * @version v1.0 2023-02-21 15:08
 */
@Slf4j
public final class RequestContext {

    public static AttributeKey<RequestContext> RECORD = AttributeKey.valueOf("RECORD");

    private final Map<Integer, SendTask> sendRequests = new ConcurrentHashMap<>();


    /**
     * 记录发出的请求，以保证收到响应时，能发给对应的业务处理处。<br/>
     * @param msgId   通信消息 id
     */
    public void addNewTask(int msgId) {
        sendRequests.put(msgId, new SendTask(msgId));
    }

    /**
     * 阻塞式获取原始的响应 command，此时的对象还没有做反序列化，后续使用该对象是时，
     * 需要手工做好反序列化。参考：{@link CommandDispatcher#dispatch(Channel, RemotingCommand)}
     * @param msgId 消息 id
     * @return 响应 command 对象
     */
    public DefaultResponseCommand getResponseCommand(int msgId) {
        SendTask sendTask = sendRequests.get(msgId);
        try {
            if (sendTask == null)
                throw new RuntimeException("该请求未能正确发出！或已经过期！" +
                        "也有可能是在请求发送阶段发生了某种非预期的阻塞？");
            return sendTask.getResponse(DefaultConfig.DEFAULT_CONSUMER_WAIT_MILLIS);
        } catch (Throwable t) {
            return createFailedCommand(msgId, t);
        }
    }

    /**
     * io 线程收到响应后，会提交到 context 中， 供请求侧获取<br/>
     * 参考：{@link CommandHandler#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)}
     */
    public void commitResponseCommand(DefaultResponseCommand responseCommand) {
        int msgId = responseCommand.getMessageId();
        SendTask sendTask = sendRequests.get(msgId);

        // == null 表示该响应早已失效（可能是超时）
        if (sendTask== null)
            return;

        sendTask.putResponseCommand(responseCommand);
    }

    /**
     * 手工放回失败的响应 Command，可以在任何请求出错的地方放入
     */
    public void commitFailedResponseCommand(int msgId, Throwable t) {
        SendTask sendTask = sendRequests.get(msgId);
        if (sendTask == null)
            return;

        sendTask.putResponseCommand(createFailedCommand(msgId, t));
    }

    /**
     * 构造一个失败的响应，通常发生在内部响应失败的场景下，直接返回一个失败。<br/>
     * @param msgId 响应 id
     * @param t     异常
     * @return 失败的响应，包裹了具体的异常
     */
    private DefaultResponseCommand createFailedCommand(int msgId, Throwable t) {
        BaseResponse response = new BaseResponse(t);
        response.setStatus(ResponseStatus.FAILED);
        response.setReturnMsg("获取响应失败！ 错误信息: " + t.getMessage());
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(msgId);
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

    /**
     * 移除调用任务
     * @param msgId 消息 id
     */
    public void removeSuchTask(int msgId){
        this.sendRequests.remove(msgId);
    }

    /**
     * 优雅关闭！
     * 相对优雅地移除所有的任务
     * @param timeoutMillis 关闭时应该等待的过期时间
     */
    public synchronized void RemoveAllTasks(long timeoutMillis){
        if (timeoutMillis < 0) throw new IllegalArgumentException("非法的过期时间: " + timeoutMillis);

        long time = System.currentTimeMillis();

        while(System.currentTimeMillis() - time > timeoutMillis){
            if (!hasTasks()){
                log.trace("当前已发起的任务均在限定时间内执行完毕!");
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
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
    private void unsafeRemoveAll(){
        if (hasTasks())
            log.warn("当前仍存有未执行完的任务");
        this.sendRequests.clear();
    }



    /**
     * 判断当前是否仍有请求待发送或未处理完，该方法主要用于优雅停机
     * @return 如果当前有正在处理的请求，则返回 true
     */
    public boolean hasTasks(){
        return sendRequests.size() > 0;
    }



    /**
     * 调用任务类
     */
    private static class SendTask {

        private final int messageId;

        private DefaultResponseCommand responseCommand;

        private final CountDownLatch latch = new CountDownLatch(1);

        public SendTask(int messageId) {
            this.messageId = messageId;
        }

        public DefaultResponseCommand getResponse(long timeoutMillis) {
            // 在超时时间内做等待
            try {
                boolean ok = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                if (!ok)
                    throw new RuntimeException("等待响应超时！");
            } catch (InterruptedException ignored) {
            }

            return responseCommand;
        }

        public void putResponseCommand(DefaultResponseCommand responseCommand) {
            this.responseCommand = responseCommand;
            this.latch.countDown(); // 保证请求处结束阻塞
        }
    }

}