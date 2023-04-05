package me.yq.remoting.processor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.biz.Message;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BizCode;
import me.yq.common.ResponseStatus;
import me.yq.remoting.transport.process.CommandHandler;
import me.yq.support.ChatClient;
import me.yq.utils.AssertUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

/**
 * MessageReceivedProcessor 测试类，主要覆盖的功能点：
 * 1. 接受到消息后，能够将消息提交给客户端
 * 2. 接收消息失败，能够返回接受失败的响应
 */
@ExtendWith(MockitoExtension.class)
class MessageReceivedProcessorTest {


    private final EmbeddedChannel channel = new EmbeddedChannel();

    @Mock
    private final ChatClient clientMock = Mockito.mock(ChatClient.class);

    @Mock
    private CommandHandler commandHandler = Mockito.mock(CommandHandler.class);

    private ChannelHandlerContext ctx;

    private Message message;

    private BaseRequest request;


    @BeforeEach
    void setUp() {

        // 组装流水线
        channel.pipeline().addLast(commandHandler);
        ctx = channel.pipeline().context(commandHandler);

        // 准备 mock 数据
        User from = new User();
        from.setName("from");
        User to = new User();
        to.setName("to");
        message = new Message(from, to, "hello");
        request = new BaseRequest(BizCode.Messaging, message);
    }

    @AfterEach
    void tearDown() {
        channel.close();
    }


    @Test
    @DisplayName("测试接受消息，成功处理")
    void messageReceived_success() throws Exception {

        MessageReceivedProcessor messageReceivedProcessor = new MessageReceivedProcessor(clientMock);


        // 定义 channelRead 后，会调用 messageReceivedProcessor
        Mockito.doAnswer(invocation->{
            messageReceivedProcessor.processRequest(ctx,1,request);
            return null;
        }).when(commandHandler).channelRead(ctx,request);


        // 开始测试
        channel.writeInbound(request);
        Mockito.verify(clientMock).acceptMsg(message.getFromUser(), message.getMsg());

        Object o = channel.readOutbound();
        AssertUtils.assertResponseStatus(o, ResponseStatus.SUCCESS);
    }


    @Test
    @DisplayName("测试接受消息，但是出现了异常")
    void messageReceived_fail() throws Exception {

        MessageReceivedProcessor messageReceivedProcessor = new MessageReceivedProcessor(clientMock, Collections.singletonList(() -> {
            throw new RuntimeException("Mock Exception");
        }),null);


        // 定义 channelRead 后，会调用 messageReceivedProcessor
        Mockito.doAnswer(invocation->{
            messageReceivedProcessor.processRequest(ctx,1,request);
            return null;
        }).when(commandHandler).channelRead(ctx,request);


        // 开始测试
        channel.writeInbound(request);

        Object o = channel.readOutbound();
        AssertUtils.assertResponseStatus(o, ResponseStatus.FAILED);
    }
}