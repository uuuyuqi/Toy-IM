package me.yq.remoting.transport.deliver.process;

import me.yq.remoting.transport.command.DefaultRequestCommand;
import me.yq.remoting.transport.command.DefaultResponseCommand;
import me.yq.remoting.transport.support.BaseRequest;
import me.yq.remoting.transport.support.BaseResponse;
import me.yq.remoting.utils.DirectThreadPool;
import me.yq.remoting.transport.support.constant.BizCode;
import me.yq.remoting.transport.support.constant.ResponseStatus;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 通信对象处理管理器，子类实现时，一般用作处理器的管理类。对外可以使用本类的{@link #acceptAndProcess(Channel, DefaultRequestCommand)}
 * 方法来接收来自通信层{@link CommandHandler}的请求，并对其进行处理，一般的处理流程就是交给内部维护的处理器来处理。
 * @author yq
 * @version v1.0 2023-02-21 10:36
 */
public class RequestProcessorManager {

    private final Executor processThreadPool;

    private static final Executor DEFAULT_EXECUTOR = DirectThreadPool.getInstance();

    /**
     * 默认使用当前线程作为 biz 线程池
     */
    public RequestProcessorManager(){
        this(DEFAULT_EXECUTOR);
    }

    public RequestProcessorManager(Executor processThreadPool) {
        this.processThreadPool = processThreadPool;
    }


    private final Map<BizCode, RequestProcessor> processors = new ConcurrentHashMap<>(10);



    /**
     * 注册相应的协议处理器，用于处理相匹配的特定枚举类型的通信对象
     */
    public void registerProcessor(BizCode bizCode, RequestProcessor processor) {
        processors.putIfAbsent(bizCode, processor);
    }


    /**
     * 按照某种类型的枚举，选择相应的处理器
     */
    public RequestProcessor getProcessor(BizCode bizCode) {
        boolean notExist = bizCode == null || !processors.containsKey(bizCode);
        return notExist ? null : processors.get(bizCode);
    }


    /**
     * 接收并处理请求，一般是观察者模式来实现消息委派处理，每个观察者都需要维护，而且需要子类去实现<br/>
     * 这个地方也是数据处理并返回的地方，所以需要传入 channel 信息，以便直接进行响应的返回<br/>
     * 值得注意的是，<b>这个地方会进行通信对象的序列化和反序列化！</b>
     * (<b>note that it will serialize and deserialize the communication object here！</b>)
     *
     * @param channel 通信信息来自的通道
     * @param requestCommand 原生的通信对象，传入时还没有做任何处理
     */
    public void acceptAndProcess(Channel channel, DefaultRequestCommand requestCommand) {
        getExecutor().execute(()->{
            // process request
            requestCommand.resolveRemoting();  // 反序列化
            BaseRequest request = requestCommand.getAppRequest();
            RequestProcessor processor = getProcessor(request.getBizCode());
            BaseResponse response;
            try {
                response = processor.process(new RequestWrapper(request,channel));
            }catch (Exception e) {
                response = new BaseResponse(ResponseStatus.FAILED,e.getMessage(),e);
            }

            // prepare response
            DefaultResponseCommand responseCommand = new DefaultResponseCommand(requestCommand.getMessageId());
            responseCommand.setAppResponse(response);
            responseCommand.toRemotingCommand(); // 序列化
            channel.writeAndFlush(responseCommand);
        });

    }


    /**
     * 提高可测试性
     * @return 线程池
     */
    public Executor getExecutor() {
        return this.processThreadPool;
    }
}
