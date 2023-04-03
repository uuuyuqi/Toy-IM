package me.yq.remoting.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.SystemException;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.support.RequestFutureMap;

/**
 * 消息发送委托
 *
 * @author yq
 * @version v1.0 2023-02-22 17:56
 */
public class CommandSendingDelegate {


    //================== 发送请求 ==================
    /**
     * 同步发送请求
     *
     * @param channel 接收消息的 channel
     * @param request 待发送的业务信息
     * @return 响应数据
     */
    public static BaseResponse sendRequestSync(Channel channel, BaseRequest request, long timeoutMillis) {
        ensureChannelHealthy(channel);

        DefaultResponseCommand responseCommand = sendRequestSync0(channel, wrapSerializedRequestCommand(request), timeoutMillis);
        if (responseCommand == null)
            return null;

        responseCommand.deserialize(); // 反序列化
        return responseCommand.getAppResponse();
    }


    /**
     * 同步发送消息内部处理，在该方法中，会将请求序列化，并发送到远程端。
     * 此外，请求的基本信息会记录在 channel 的 futureMap 中，以便在接收到响应时，能够找到对应的请求。
     *
     * @param channel 待接收请求的 channel
     * @param requestCommand 待发送的请求
     * @param timeoutMillis 等待响应超时时间
     * @return 响应数据
     */
    private static DefaultResponseCommand sendRequestSync0(Channel channel, DefaultRequestCommand requestCommand, long timeoutMillis) {

        // record request
        int requestId = requestCommand.getMessageId();
        RequestFutureMap futureMapInChannel = channel.attr(ChannelAttributes.CHANNEL_REQUEST_FUTURE_MAP).get();
        futureMapInChannel.addNewFuture(requestId);

        try {
            channel.writeAndFlush(requestCommand).addListener(
                    future -> {
                        if (!future.isSuccess()) {
                            String errMsg = "消息发送失败!  异常信息： " + future.cause().getMessage();
                            futureMapInChannel.commitFailedResponseCommand(requestId, future.cause());
                            throw new RuntimeException(errMsg);
                        }
                    }
            );
        } catch (Exception e) {
            futureMapInChannel.commitFailedResponseCommand(requestId, e);
        }

        return futureMapInChannel.takeResponseCommand(requestId,timeoutMillis);
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
        requestCommand.serialize();
        return requestCommand;
    }



    //================== 发送响应 ==================
    /**
     * 根据业务响应消息，生成一个已经序列化后的通信对象，并进行返回。
     * 注意，如果该响应是不需要返回的，则该方法不会完整执行。
     * @param channel  接收消息的 channel
     * @param reqId    请求id，表示这个通信响应对象是对哪个请求的响应
     * @param response 业务响应对象
     */
    public static void sendResponseOneway(ChannelHandlerContext ctx, int reqId, BaseResponse response) {
        if (response.getStatus() == ResponseStatus.NO_NEED_RESPONSE)
            return;

        ensureChannelHealthy(ctx.channel());

        ctx.writeAndFlush(wrapSerializedResponseCommand(reqId,response)).addListener(
                future -> {
                    if (!future.isSuccess()) {
                        String errMsg = "消息发送失败!  异常信息： " + future.cause().getMessage();
                        throw new RuntimeException(errMsg);
                    }
                }
        );
    }

    /**
     *
     * 可以将原始响应对象包装成一个 通信响应对象
     *
     * @param reqId   请求id，表示这个通信响应对象是对哪个请求的响应
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
     * @param channel 待发送消息的 channel
     * @throws SystemException channel 状态相关的异常
     */
    private static void ensureChannelHealthy(Channel channel) throws SystemException {
        if (channel == null || !channel.isActive())
            throw new SystemException(new IllegalStateException("当前无法发送消息，请检查 channel 状态: " + channel));
        else if (!channel.isWritable())
            throw new SystemException(new IllegalStateException("写入太多可能已经造成了 oom？请检查 channel 状态:" + channel));
    }
}
