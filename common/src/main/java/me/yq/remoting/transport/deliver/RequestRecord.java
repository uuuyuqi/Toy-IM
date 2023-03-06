package me.yq.remoting.transport.deliver;

import io.netty.channel.Channel;
import me.yq.remoting.transport.command.DefaultRequestCommand;
import me.yq.remoting.transport.command.DefaultResponseCommand;
import me.yq.remoting.transport.command.RemotingCommand;
import me.yq.remoting.transport.constant.DefaultConfig;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.transport.support.constant.ResponseStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 请求调用上下文，主要功能有 2
 * - 请求处理器会在此进行 countdown 等待，响应处理器会告知哪个请求已经等待完毕
 * - 会在此记录请求的 id，以实现线上乱序传输！提升效率！具体的行为在 {@link CommandDispatcher#dispatch(Channel, RemotingCommand)}
 *
 * @author yq
 * @version v1.0 2023-02-21 15:08
 */
public final class RequestRecord {

    static private final RequestRecord INSTANCE = new RequestRecord();

    private RequestRecord() {
    }

    static public RequestRecord getInstance() {
        return INSTANCE;
    }

    /**
     * 缓存最近的发送任务
     */
    private final Map<Channel, RequestContext> requestContextCache = new ConcurrentHashMap<>();



    @SuppressWarnings("check")
    /**
     * 记录发出的请求，以保证收到响应时，能发给对应的业务处理处。<br/>
     * 参考：{@link CommandDispatcher#dispatch(Channel, RemotingCommand)}<br/>
     * 参考：{@link CommandSendingDelegate#sendCommandSync(Channel, DefaultRequestCommand)}<br/>
     *
     * @param channel 该任务发往哪里
     * @param msgId   通信消息 id
     */
    public void registerTask(Channel channel, int msgId) {
        RequestContext context = requestContextCache.computeIfAbsent(channel, RequestContext::new);
        context.putTask(msgId);
    }

    /**
     * 阻塞式获取原始的响应 command，此时的对象还没有做反序列化，后续使用该对象是时，
     * 需要手工做好反序列化。参考：{@link CommandDispatcher#dispatch(Channel, RemotingCommand)}
     * @param channel 请求去往的 channel
     * @param msgId 消息 id
     * @return 响应 command 对象
     */
    public DefaultResponseCommand getResponseCommand(Channel channel, int msgId) {
        try {
            if (isRequestExpired(channel,msgId))
                return createFailedCommand(msgId,"该请求未能正确发出！或已经过期！也有可能是在请求发送阶段发生了某种非预期的阻塞？");

            return requestContextCache.get(channel).getResponseCommand(msgId);
        } catch (Throwable t) {
            return createFailedCommand(msgId, t);
        }
    }

    /**
     * 放回正常的响应 Command
     */
    public void putResponseCommand(Channel channel, int msgId, DefaultResponseCommand responseCommand) {
        // 超时的响应直接丢弃
        if (isRequestExpired(channel, msgId))
            return;
        RequestContext context = requestContextCache.get(channel);
        context.putResponseCommand(msgId,responseCommand);
    }

    /**
     * 放回失败的响应 Command
     */
    public void putFailedResponseCommand(Channel channel, int msgId, Throwable t) {
        if (isRequestExpired(channel, msgId))
            return;
        RequestContext context = requestContextCache.get(channel);
        context.putResponseCommand(msgId,createFailedCommand(msgId,t));
    }

    /**
     * 放回失败的响应 Command
     */
    public void putFailedResponseCommand(Channel channel, int msgId, String errorMessage) {
        if (isRequestExpired(channel, msgId))
            return;
        RequestContext context = requestContextCache.get(channel);
        context.putResponseCommand(msgId,createFailedCommand(msgId,errorMessage));
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
     * 检测当前响应是否已经过期，实际上应该在服务端丢弃的，这个版本先不写那么复杂了
     * @param channel 响应来自的 channel
     * @param requestId 响应对应的请求 id
     * @return 如果响应已经过期，则返回 true
     */
    private boolean isRequestExpired(Channel channel,int requestId){
        RequestContext context = requestContextCache.get(channel);
        return (context == null || context.isRequestExpired(requestId));
    }

    /**
     * 移除调用任务
     * @param channel 调用的 channel
     * @param msgId 消息 id
     */
    public void removeTask(Channel channel,int msgId){
        this.requestContextCache.get(channel).removeTask(msgId);
    }


    /**
     * 清空 channel 上所有的调用请求，清空之前，会等待一定的时间，类似于 netty 的安静周期。
     * 相当于一定程度上的优雅关闭
     * @param channel 待清空的 channel
     * @param timeoutMillis 超时时间，单位毫秒
     */
    public void removeAllTask(Channel channel,long timeoutMillis){
        if (!this.requestContextCache.isEmpty()) {
            try {
                TimeUnit.MILLISECONDS.sleep(timeoutMillis);
            } catch (InterruptedException ignored) {
                // 极端情况下才会可能触发：在优雅停机阶段，某处触发了 thread.interrupt()
                throw new IllegalStateException("发现有未处理的请求，但是处理线程已经关闭了！");
            }
        }

        this.requestContextCache.remove(channel);
    }


    /**
     * Request 上下文，包装了当前 channel 的调用情况，也作为请求和响应的沟通桥梁。
     * todo
     *  requets task 的包装，目的是包上调用次数 + 当前 channel 上的任务
     */
    private class RequestContext{

        private final Channel channel;

        private final Map<Integer, SendTask> sendTasks = new ConcurrentHashMap<>();

        public RequestContext(Channel channel) {
            this.channel = channel;
        }

        /**
         * 获取 channel 上，当前已经发送，但还没有响应的请求数
         * @return
         */
        public int getCurrentRequests() {
            return sendTasks.size();
        }

        public Channel getChannel() {
            return channel;
        }

        /**
         * 放入新任务
         * @param msgId 消息 id
         */
        public void putTask(int msgId){
            this.sendTasks.put(msgId, new SendTask(msgId));
        }

        /**
         * 查看当前请求是否已经过期，实现的方式就是查看该请求是否已经被移除
         * @param msgId 请求 id
         * @return 如果未过期（未移除），则返回返回 true
         */
        public boolean isRequestExpired(int msgId){
            return !sendTasks.containsKey(msgId);
        }

        /**
         * 根据请求 id 获取响应
         */
        public DefaultResponseCommand getResponseCommand(int msgId){
            SendTask task = this.sendTasks.get(msgId);
            return task.getResponseCommand();
        }

        /**
         * 根据请求 id 放入响应
         */
        public void putResponseCommand(int msgId, DefaultResponseCommand responseCommand){
            SendTask task = this.sendTasks.get(msgId);
            task.putResponseCommand(responseCommand);
        }

        /**
         * 根据请求 id 移除请求
         */
        public void removeTask(int msgId){
            this.sendTasks.remove(msgId);
        }



        /**
         * 调用任务类
         */
        private class SendTask {

            private final int messageId;

            private DefaultResponseCommand responseCommand;

            private final CountDownLatch latch = new CountDownLatch(1);

            public SendTask(int messageId) {
                this.messageId = messageId;
            }

            public DefaultResponseCommand getResponseCommand() {
                // 在超时时间内做等待
                try {
                    boolean ok = latch.await(DefaultConfig.DEFAULT_CONSUMER_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                    if (!ok)
                        return RequestRecord.this.createFailedCommand(messageId,"等待响应超时！");
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


}
