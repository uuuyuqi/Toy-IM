package me.yq.remoting.transport.process;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.remoting.transport.CommandSendingDelegate;

import java.util.List;

/**
 * 请求处理器模板类
 *
 * @author yq
 * @version v1.0 2023-02-21 14:13
 */
@Slf4j
public abstract class RequestProcessor {

    /**
     * 业务处理器前置任务，可以认为时业务处理的扩展点
     */
    private final List<Runnable> preTasks;

    private final List<Runnable> postTasks;

    public RequestProcessor() {
        this.preTasks = null;
        this.postTasks = null;
    }

    public RequestProcessor(List<Runnable> preTasks, List<Runnable> postTasks) {
        this.preTasks = preTasks;
        this.postTasks = postTasks;
    }

    /**
     * processor 在做业务处理的时候可能会用到 channel 对象，比如用户登陆，就需要 processor 收集用户的 channel 信息。
     * 而 requestProcessor 本身时单例的，不能直接使用 channel 属性，否则会有严重的线程安全问题！
     */
    private final ThreadLocal<Channel> channelLocal = new ThreadLocal<>();

    /**
     * 处理器业务
     *
     * @param ctx       当前 channelHandlerContext
     * @param requestId 请求id
     * @param request   实际业务请求
     */
    public void processRequest(ChannelHandlerContext ctx, int requestId, BaseRequest request) {

        // 1.将 channel 信息放入 threadLocal 中
        channelLocal.set(ctx.channel());

        // 2.处理 pre 扩展点
        if (preTasks != null) {
            for (Runnable task : preTasks) {
                task.run();
            }
        }

        // 3.处理业务
        BaseResponse response;
        try {
            response = doProcess(request);
        } catch (Exception e) {
            response = new BaseResponse(ResponseStatus.FAILED, e.getMessage(), e);
        }

        // 4.发送响应
        CommandSendingDelegate.sendResponseOneway(ctx, requestId, response);

        // 5.处理 post 扩展点
        if (postTasks != null) {
            for (Runnable task : postTasks) {
                task.run();
            }
        }

        // 6.清除 threadLocal！
        channelLocal.remove();
    }


    /**
     * 处理业务请求
     *
     * @param request 业务请求
     */
    abstract public BaseResponse doProcess(BaseRequest request);

    public ThreadLocal<Channel> getChannelLocal() {
        return channelLocal;
    }
}
