package me.yq.remoting.processor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.biz.Notice;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BizCode;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.SystemException;
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
 * NoticeFromServerProcessor 测试类，主要覆盖的功能点：
 * 1. 可以正常将通知提交给客户端
 */
@ExtendWith(MockitoExtension.class)
class NoticeFromServerProcessorTest {
    private final EmbeddedChannel channel = new EmbeddedChannel();

    @Mock
    private ChatClient clientMock;

    @Mock
    private CommandHandler commandHandler;

    private ChannelHandlerContext ctx;

    private Notice notice;

    private BaseRequest request;


    @BeforeEach
    void setUp() {
        // 组装流水线
        channel.pipeline().addLast(commandHandler);
        ctx = channel.pipeline().context(commandHandler);

        // 准备 mock 数据
        notice = new Notice(123, "===notice===", "work hard or you may lose this job");
        request = new BaseRequest(BizCode.Noticing, notice);
    }

    @AfterEach
    void tearDown() {
        channel.close();
    }

    @Test
    @DisplayName("测试接收到服务端的通知")
    void testAcceptNotice_success() throws Exception {
        NoticeFromServerProcessor noticeFromServerProcessor = new NoticeFromServerProcessor(clientMock);

        // 定义 channelRead 后，会调用 messageReceivedProcessor
        Mockito.doAnswer(invocation -> {
            noticeFromServerProcessor.processRequest(ctx, 1, request);
            return null;
        }).when(commandHandler).channelRead(ctx, request);

        Mockito.when(clientMock.getCurrentUser()).thenReturn(new User(123, "test"));

        // 开始测试
        channel.writeInbound(request);
        Mockito.verify(clientMock).acceptNotice(notice);

        Object o = channel.readOutbound();
        AssertUtils.assertResponseStatus(o,ResponseStatus.SUCCESS);
    }

    @Test
    @DisplayName("接收通知失败，返回失败响应")
    void testAcceptNotice_fail() throws Exception {
        NoticeFromServerProcessor noticeFromServerProcessor = new NoticeFromServerProcessor(
                clientMock,
                Collections.singletonList(()->{
                    throw new SystemException("test exception");
                }),
                null);

        // 定义 channelRead 后，会调用 messageReceivedProcessor
        Mockito.doAnswer(invocation -> {
            noticeFromServerProcessor.processRequest(ctx, 1, request);
            return null;
        }).when(commandHandler).channelRead(ctx, request);

        // 开始测试
        channel.writeInbound(request);

        Object o = channel.readOutbound();
        AssertUtils.assertResponseStatus(o,ResponseStatus.FAILED);
    }

}