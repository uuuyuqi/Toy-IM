package me.yq.remoting.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.BaseResponseFuture;
import me.yq.common.exception.SystemException;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.utils.NamedThreadFactory;

import java.util.concurrent.*;


/**
 * 消息发送委托
 *
 * @author yq
 * @version v1.0 2023-02-22 17:56
 */
@Slf4j
public class CommandSendingDelegate {

    /**
     * 用于传递 IO 线程是否发消息成功的标志。在阻塞式发送的情况下可以准确传递。
     * 后期增加异步发送方式的情况下可能需要再考虑下。
     */
    private static final ThreadLocal<SendState> SEND_SUCCESS_RECORD = ThreadLocal.withInitial(SendState::new);

    /**
     * 后台调度线程池，专用于监控异步调用的情况下，请求是否超时
     */
    private static final ScheduledExecutorService TimeoutChecker = new ScheduledThreadPoolExecutor(1,new NamedThreadFactory("TimeoutChecker",true));


    //================== 发送请求 ==================
    /**
     * 同步发送请求，实际上是对异步调用的改造，手工在这里等待。
     *
     * @param channel       接收消息的 channel
     * @param request       待发送的业务信息
     * @param timeoutMillis 等待响应超时时间
     * @return 响应数据
     */
    public static BaseResponse sendRequestSync(Channel channel, BaseRequest request, long timeoutMillis) {
        RequestFuture future = internalSendRequestAsync(channel, request,null);

        DefaultResponseCommand responseCommand = future.acquireAndClose(timeoutMillis);

        if (responseCommand == null)
            return null;

        responseCommand.deserialize();
        return responseCommand.getAppResponse();
    }

    /**
     * 异步发送请求，发送完之后会得到一个 future，可以通过 future 获取响应。
     * @param channel 接收消息的 channel
     * @param request 待发送的业务信息
     * @return future，该 future 可以阻塞式获取响应
     */
    public static BaseResponseFuture sendRequestAsync(Channel channel, BaseRequest request) {
        RequestFuture future = internalSendRequestAsync(channel, request, null);
        return new BaseResponseFuture(future);
    }


    /**
     * 异步发送请求。在该方法中，会将请求序列化，并发送到远程端。
     * 发出的请求会放在 channel attr 中，等待响应到来之时，会将响应提交，并告诉 biz thread 去处理 callback。
     *
     * @param channel       接收消息的 channel
     * @param request       待发送的业务信息
     * @param timeoutMillis 等待响应超时时间，为 -1 表示用不超时
     * @param callback      回调函数
     * @param executor      回调函数的执行线程池
     */
    public static void sendRequestCallback(Channel channel, BaseRequest request, long timeoutMillis, Callback callback, Executor executor) {

        RequestFuture future = internalSendRequestAsync(channel, request, callback);

        // 提交超时检测任务
        ScheduledFuture<?> scheduledFuture = TimeoutChecker.schedule(() -> {
            // 超时前再确认下是否已经有响应了
            DefaultResponseCommand responseCommand = future.acquireAndClose(0);
            if (responseCommand == null)
                callback.onTimeout();

        }, timeoutMillis, TimeUnit.MILLISECONDS);

        ((CallbackCarryingRequestFuture)future).setScheduledFuture(scheduledFuture);
    }


    /**
     * 该方法是内部的核心工具方法。用途是发送请求，并获取请求的 future，该方法可以供业务层调用并获取 future，
     * future 方式调用和 sync 调用都依赖了本方法。可参考 {@link #sendRequestSync} 和 {@link #sendRequestCallback}
     *
     *
     * @param channel 待发送请求的 channel
     * @param request 待发送的请求
     * @param callback 回调函数，该参数决定了获取的 future 的类型是 普通的阻塞式 future 还是 callback 形式
     * @return 请求的 future
     */
    private static RequestFuture internalSendRequestAsync(Channel channel, BaseRequest request, Callback callback) {
        ensureChannelHealthy(channel);

        DefaultRequestCommand requestCommand = wrapSerializedRequestCommand(request);
        int requestId = requestCommand.getMessageId();
        RequestFutureMap futureMapInChannel = channel.attr(ChannelAttributes.CHANNEL_REQUEST_FUTURE_MAP).get();
        RequestFuture future = callback == null ? new DefaultRequestFuture(requestId,futureMapInChannel) : new CallbackCarryingRequestFuture(requestId, futureMapInChannel,callback);
        futureMapInChannel.addNewFuture(future);

        try {
            channel.writeAndFlush(requestCommand).addListener(
                    f -> {
                        if (!f.isSuccess()) {
                            String errMsg = "消息发送失败!  异常信息： " + f.cause().getMessage();
                            future.putFailedResponse(f.cause());
                            futureMapInChannel.removeSuchFuture(requestId);
                            throw new SystemException(errMsg);
                        }
                    }
            );
        } catch (Exception e) {
            future.putFailedResponse(e);
        }
        return future;
    }



    /**
     * 单向发送请求，这种发送方式并不关心请求结果如何。该方式虽然不会等待结果，但是等待消息的成功发送。
     *
     * @param channel 接收消息的 channel
     * @param request 待发送的业务信息
     */
    public static void sendRequestOneway(Channel channel, BaseRequest request, long timeoutMillis) {

        ensureChannelHealthy(channel);

        final SendState sendState = SEND_SUCCESS_RECORD.get();
        channel.writeAndFlush(wrapSerializedRequestCommand(request)).addListener(
                future -> {
                    if (!future.isSuccess()) {
                        sendState.setState(SendStates.FAILED);
                        sendState.setThrowable(future.cause());
                    } else
                        sendState.setState(SendStates.SUCCESS);
                }
        );

        // 不停等待10ms 直到超时或者发送状态是成功
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (sendState.getState() == SendStates.SUCCESS)
                return;
            try {
                TimeUnit.MILLISECONDS.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }

        if (sendState.getState() == SendStates.FAILED) {
            String errMsg = "发送消息失败：[" + request.getAppRequest() + "]\n异常信息：" + sendState.getThrowable().getMessage();
            log.error(errMsg);
            throw new SystemException(errMsg, sendState.getThrowable());
        } else if (sendState.getState() == SendStates.SENT) {
            String errMsg = "发送消息超时：[" + request.getAppRequest() + "]";
            log.error(errMsg);
            throw new SystemException(errMsg);
        }
    }


    /**
     * 根据业务请求消息，生成一个已经序列化后的通信对象。该方法通常在即将进行远程通信时调用，
     * 可以将原始的业务请求对象，包装成一个通信对象。
     *
     * @param request 待包装的业务对象
     * @return 已经序列化后的远程通信对象
     */
    private static DefaultRequestCommand wrapSerializedRequestCommand(BaseRequest request) {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setAppRequest(request);
        //log.info("序列化前的请求对象：{}\n序列化前时间:{}", request, CommonUtils.now());
        requestCommand.serialize();
        //log.info("序列化后时间:{}",  CommonUtils.now());
        return requestCommand;
    }


    //================== 发送响应 ==================

    /**
     * 根据业务响应消息，生成一个已经序列化后的通信对象，并进行返回。
     * 注意，如果该响应是不需要返回的，则该方法不会完整执行。
     *
     * @param ctx      对应的 channelHandlerContext
     * @param reqId    请求id，表示这个通信响应对象是对哪个请求的响应
     * @param response 业务响应对象
     */
    public static void sendResponseOneway(ChannelHandlerContext ctx, int reqId, BaseResponse response) {

        ensureChannelHealthy(ctx.channel());

        ctx.writeAndFlush(wrapSerializedResponseCommand(reqId, response)).addListener(
                future -> {
                    if (!future.isSuccess()) {
                        String errMsg = "消息发送失败!  异常信息： " + future.cause().getMessage();
                        throw new RuntimeException(errMsg);
                    }
                }
        );
    }

    /**
     * 可以将原始响应对象包装成一个 通信响应对象
     *
     * @param reqId    请求id，表示这个通信响应对象是对哪个请求的响应
     * @param response 待包装的业务对象
     * @return 已经序列化后的远程通信对象
     */
    private static DefaultResponseCommand wrapSerializedResponseCommand(int reqId, BaseResponse response) {
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(reqId);
        responseCommand.setAppResponse(response);
        responseCommand.serialize();
        return responseCommand;
    }


    /**
     * 检测 channel 装态是否正常，是否可以发送消息。如果不可以发送消息，则本方法会直接报错。
     *
     * @param channel 待发送消息的 channel
     * @throws SystemException channel 状态相关的异常
     */
    private static void ensureChannelHealthy(Channel channel) throws SystemException {
        if (channel == null || !channel.isActive())
            throw new SystemException(new IllegalStateException("当前无法发送消息，请检查 channel 状态: " + channel));
        else if (!channel.isWritable())
            throw new SystemException(new IllegalStateException("写入太多可能已经造成了 oom？请检查 channel 状态:" + channel));
    }


    /**
     * 用于记录消息发送状态的类，主要用于包装消息的发送状态
     */
    private static class SendState {
        private SendStates state = SendStates.SENT;

        private Throwable throwable;

        public SendStates getState() {
            return state;
        }

        public void setState(SendStates state) {
            this.state = state;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }
    }

    /**
     * 消息发送状态枚举，存在三种情况：
     * 1.SENT     已进行发送
     * 2.SUCCESS  发送成功
     * 3.FAILED   发送失败
     */
    private enum SendStates {
        SENT,
        SUCCESS,
        FAILED
    }
}
