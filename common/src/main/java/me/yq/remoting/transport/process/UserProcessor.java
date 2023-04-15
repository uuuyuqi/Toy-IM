package me.yq.remoting.transport.process;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.SystemException;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.command.RemotingCommand;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.transport.RequestFutureMap;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * requestProcessor 聚合对象，持有业务处理器和对应业务码的映射。
 * 可以统一对外接收业务层数据，根据业务数据携带的业务码，选择出合适的业务处理器，进行业务的处理
 *
 * @author yq
 * @version v1.0 2023-03-28 20:29
 */
@Slf4j
public class UserProcessor {


    private ThreadPoolExecutor bizThreadPool;

    private final Map<Byte, RequestProcessor> bizProcessors = new ConcurrentHashMap<>(8);


    /**
     * 记录当前正在处理的请求数
     */
    private final AtomicInteger currentRequestCounts = new AtomicInteger(0);


    public UserProcessor(ThreadPoolExecutor bizThreadPool) {
        this.bizThreadPool = Objects.requireNonNull(bizThreadPool);
    }


    public void registerBizProcessor(byte code, RequestProcessor processor) {
        RequestProcessor processorOld = this.bizProcessors.put(code, Objects.requireNonNull(processor));
        if (processorOld != null)
            throw new UnsupportedOperationException("不允许重复注册 processors");
    }

    public void processCommand(ChannelHandlerContext ctx, RemotingCommand command) {
        if (command instanceof DefaultRequestCommand) {
            DefaultRequestCommand requestCommand = (DefaultRequestCommand) command;
            processRequest(ctx, requestCommand);
        } else if (command instanceof DefaultResponseCommand) {
            DefaultResponseCommand responseCommand = (DefaultResponseCommand) command;
            processResponse(ctx, responseCommand);
        }
    }


    /**
     * 处理业务请求
     *
     * @param ctx            当前 netty handler context，该参数可以用来做消息的回传
     * @param requestCommand 待处理的请求
     */
    private void processRequest(ChannelHandlerContext ctx, DefaultRequestCommand requestCommand) {
        // process in IO thread
        // 当系统处理停机状态时，不会再处理客户端发来的新请求
        ChannelAttributes.ChannelState channelState = ctx.channel().attr(ChannelAttributes.CHANNEL_STATE).get();
        if (channelState == ChannelAttributes.ChannelState.CANNOT_REQUEST) {
            BaseResponse response = new BaseResponse(ResponseStatus.SERVER_ERROR, "系统正在停机，无法接收请求！", null);
            CommandSendingDelegate.sendResponseOneway(ctx, requestCommand.getMessageId(), response);
        } else if (channelState == ChannelAttributes.ChannelState.CLOSED) {
            // todo 后面这个地方应该是 do noting，打这个日志是方便测试
            log.warn("检测到 closed 状态的 channel 仍然发来信息! 请检查 channel 状态变更为 closed 后的代码逻辑");
        }

        // process in BIZ thread
        else if (channelState == null /* request for the first time */
                || channelState == ChannelAttributes.ChannelState.CAN_REQUEST) {
            this.bizThreadPool.execute(() -> {
                // 每次处理请求时，请求计数+1
                try{
                    this.currentRequestCounts.incrementAndGet();
                    doProcessRequest(ctx, requestCommand);
                }finally {
                    this.currentRequestCounts.decrementAndGet();
                }

            });
        }
    }

    /**
     * 反序列化请求，并找到对应的业务处理器，开始真正地处理业务请求
     */
    private void doProcessRequest(ChannelHandlerContext ctx, DefaultRequestCommand requestCommand) {

        // 1. 反序列化
        requestCommand.deserialize();
        BaseRequest request = requestCommand.getAppRequest();

        // 2. 查找对应的业务处理器
        RequestProcessor processor = this.bizProcessors.get(request.getBizCode());
        if (processor == null)
            throw new SystemException("未找到交易码[" + request.getBizCode() + "]的处理器！请检查交易码的合法性或者是否设计并装配了该类型交易的处理器");

        // 3. 业务处理
        processor.processRequest(ctx, requestCommand.getMessageId(), request);
    }

    /**
     * 处理业务响应
     *
     * @param ctx             当前 netty handler context，该参数可以用来获取当前 channel，进而获取到 channel 中的 Attr
     * @param responseCommand 待处理的响应
     */
    private void processResponse(ChannelHandlerContext ctx, DefaultResponseCommand responseCommand) {
        RequestFutureMap requestFutureMap = ctx.channel().attr(ChannelAttributes.CHANNEL_REQUEST_FUTURE_MAP).get();
        // 将 response 传递给给请求处
        requestFutureMap.commitResponseCommand(responseCommand);
    }


    public AtomicInteger getCurrentRequestCounts() {
        return currentRequestCounts;
    }


    public void setBizThreadPool(ThreadPoolExecutor bizThreadPool) {
        this.bizThreadPool = bizThreadPool;
    }

    public ThreadPoolExecutor getBizThreadPool() {
        return bizThreadPool;
    }

    public Map<Byte, RequestProcessor> getBizProcessors() {
        return bizProcessors;
    }
}
