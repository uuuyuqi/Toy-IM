package me.yq.remoting.transport.process;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.SystemException;
import me.yq.remoting.transport.CommandSendingDelegate;

import java.util.List;

/**
 * 业务请求处理器模板类
 *
 * @author yq
 * @version v1.0 2023-02-21 14:13
 */
@Slf4j
public abstract class RequestProcessor {

    /**
     * 标志这个处理器是否需要做返回操作。用以区分单向处理器和有业务返回的处理器。
     * 一般 shouldReturn 的值为 false 时，通常处理结果的 response 状态码会是 OK_NO_NEED_RESPONSE
     */
    private final boolean shouldReturn;

    /**
     * 业务处理器前置任务，在每次业务请求被处理之前，就会执行这些任务
     */
    private final List<Runnable> preTasks;

    /**
     * 业务处理器后置任务，等到响应被成功返回给客户端后，才会执行这些任务
     */
    private final List<Runnable> postTasks;

    public RequestProcessor(boolean shouldReturn) {
        this(shouldReturn, null, null);
    }

    public RequestProcessor(boolean shouldReturn, List<Runnable> preTasks, List<Runnable> postTasks) {
        this.shouldReturn = shouldReturn;
        this.preTasks = preTasks;
        this.postTasks = postTasks;
    }

    /**
     * processor 在做业务处理的时候可能会用到 channel 对象，比如用户登陆，就需要 processor 收集用户的 channel 信息。
     * 而 requestProcessor 本身时单例的，不能直接使用 channel 属性，否则会有严重的线程安全问题！
     */
    private final ThreadLocal<Channel> channelLocal = new ThreadLocal<>();

    /**
     * 处理器业务模板，模板中增加了 channelLocal 的设置、扩展点的处理
     *
     * @param ctx       当前 channelHandlerContext
     * @param requestId 请求id
     * @param request   实际业务请求
     */
    public void processRequest(ChannelHandlerContext ctx, int requestId, BaseRequest request) {

        BaseResponse response = null;

        // 1.将 channel 信息放入 threadLocal 中
        channelLocal.set(ctx.channel());

        // 2.处理 pre 扩展点
        processTasks(preTasks);

        try {
            // 3.处理业务
            response = doProcess(request);

        } catch (Exception e) {
            if (shouldReturn)
                response = new BaseResponse(ResponseStatus.FAILED, e.getMessage(), e);
            else
                throw new SystemException("处理请求[" + request + "]时出现异常！", e);

        } finally {
            // 4.清除 threadLocal
            channelLocal.remove();
        }

        // 5.发送响应
        if (response == null)
            throw new RuntimeException("[{" + this.getClass().getSimpleName() + "}]处理完毕后，结果为null");

        if (response.getStatus() != ResponseStatus.OK_NO_NEED_RESPONSE)
            CommandSendingDelegate.sendResponseOneway(ctx, requestId, response);


        // 6.处理 post 扩展点
        processTasks(postTasks);
    }

    private void processTasks(List<Runnable> tasks){
        if (tasks != null) {
            for (Runnable task : tasks) {
                task.run();
            }
        }
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
