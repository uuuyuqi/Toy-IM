package me.yq.remoting.transport.process;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.transport.DefaultRequestFuture;
import me.yq.remoting.transport.RequestFuture;
import me.yq.remoting.transport.RequestFutureMap;
import me.yq.test.processors.SimpleEchoProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserProcessor 测试类，主要覆盖的功能点是：
 * 1.能注册不同的处理器，并能够将不同的请求分发给对应的处理器
 * 2.能够正常将响应提交到 channel 中的 requestFutureMap 中，完成响应对请求的关联
 * 3.在停机状态下，拒收来自客户端的请求，并返回停机报错
 * @author yq
 * @version v1.0 2023-04-07 18:53
 */
@ExtendWith(MockitoExtension.class)
class UserProcessorTest {

    private final EmbeddedChannel channel = new EmbeddedChannel();

    @Mock
    private ThreadPoolExecutor bizPool;

    private final RequestProcessor processor1 = Mockito.spy(new SimpleEchoProcessor());
    private final RequestProcessor processor2 = Mockito.spy(new SimpleEchoProcessor());
    private final RequestProcessor processor3 = Mockito.spy(new SimpleEchoProcessor());

    private ChannelHandlerContext ctx;

    private UserProcessor userProcessor;


    @BeforeEach
    void setUp() {
        userProcessor = new UserProcessor(bizPool);

        CommandHandler commandHandler = new CommandHandler(userProcessor);
        channel.pipeline().addLast(commandHandler);
        ctx = channel.pipeline().context(commandHandler);


        // 注册处理器
        userProcessor.registerBizProcessors((byte) 1, processor1);
        userProcessor.registerBizProcessors((byte) 2, processor2);
        userProcessor.registerBizProcessors((byte) 3, processor3);
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    @DisplayName("测试处理业务请求分发")
    void processRequestCommand() {

        // 将线程池中 execute 方式提交的任务都直接串行跑掉
        Mockito.doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(bizPool).execute(Mockito.any(Runnable.class));

        userProcessor.processCommand(ctx, generateSerializedRequestCommand((byte) 1));
        Mockito.verify(processor1).processRequest(Mockito.any(), Mockito.anyInt(), Mockito.any(BaseRequest.class));

        userProcessor.processCommand(ctx, generateSerializedRequestCommand((byte) 2));
        Mockito.verify(processor2).processRequest(Mockito.any(ChannelHandlerContext.class), Mockito.anyInt(), Mockito.any(BaseRequest.class));

        userProcessor.processCommand(ctx, generateSerializedRequestCommand((byte) 3));
        Mockito.verify(processor3).processRequest(Mockito.any(ChannelHandlerContext.class), Mockito.anyInt(), Mockito.any(BaseRequest.class));
    }


    @Test
    @DisplayName("测试响应的提交")
    void submitResponse() {
        int reqId = 15;
        RequestFuture future = new DefaultRequestFuture(15,null);
        RequestFutureMap requestFutureMap = new RequestFutureMap();
        requestFutureMap.addNewFuture(future);
        ctx.channel().attr(ChannelAttributes.CHANNEL_REQUEST_FUTURE_MAP).set(requestFutureMap);

        userProcessor.processCommand(ctx, generateDeserializedResponseCommand(reqId));
        DefaultResponseCommand responseCommand = future.acquireAndClose(3000);

        assertEquals(responseCommand.getAppResponse().getStatus(), ResponseStatus.SUCCESS, "响应状态不正确");
    }


    @Test
    @DisplayName("测试停机状态下的请求处理")
    void processRequestCommandWhenShutdown() {
        // 将当前 channel 设置为停机时的 CANNOT_REQUEST 状态
        ctx.channel().attr(ChannelAttributes.CHANNEL_STATE).set(ChannelAttributes.ChannelState.CANNOT_REQUEST);

        userProcessor.processCommand(ctx, generateSerializedRequestCommand((byte) 1));
        DefaultResponseCommand responseCommand = channel.readOutbound();
        assertEquals(responseCommand.getAppResponse().getStatus(), ResponseStatus.SERVER_ERROR, "响应状态不正确");
        assertTrue(responseCommand.getAppResponse().getReturnMsg().contains("停机"), "响应信息不正确，应该告知停机");
    }



    private final RequestProcessor slowProcessor = new SimpleEchoProcessor(
            Collections.singletonList(
                    ()->{
                        while (true){
                            try {
                                TimeUnit.MILLISECONDS.sleep(1);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
            ),
            null
    );



    private DefaultRequestCommand generateSerializedRequestCommand(byte bizCode) {
        BaseRequest request = new BaseRequest(bizCode, "test");
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setAppRequest(request);
        requestCommand.serialize();
        return requestCommand;
    }


    private DefaultResponseCommand generateDeserializedResponseCommand(int reqId) {
        BaseResponse response = new BaseResponse("test");
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(reqId);
        responseCommand.setAppResponse(response);
        return responseCommand;
    }
}