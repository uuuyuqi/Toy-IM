package me.yq.remoting.processor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.biz.LogInRequest;
import me.yq.biz.Notice;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BizCode;
import me.yq.common.ResponseStatus;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.remoting.config.Config;
import me.yq.remoting.config.DefaultServerConfig;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.session.Session;
import me.yq.remoting.transport.process.CommandHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;


/**
 * LogInProcessor 测试类，主要覆盖的功能点：
 * 1.用户登录成功后，能够将用户信息存储到 session 中
 * 2.用户登录失败后，能够返回登录失败的响应
 * 3.发生用户挤掉线的情况，能将老 session 下线，并为其发送安全警告
 * @author yq
 * @version v1.0 2023-04-08 15:57
 */
@ExtendWith(MockitoExtension.class)
class LogInProcessorTest {

    private final Config config = new DefaultServerConfig();

    private final SessionMap sessionMap = SessionMap.getInstanceOrCreate(config);

    private final LogInProcessor logInProcessor = new LogInProcessor(sessionMap,config);

    @AfterEach
    void tearDown() {
        sessionMap.removeAllUnSafe();
    }

    @Test
    @DisplayName("测试用户正常登录，能够将用户信息存储到 session 中，并告知用户成功登录")
    void testLoginSuccess() {
        EmbeddedChannel channel = newChannelToLogIn();
        BaseRequest loginReq = new BaseRequest(BizCode.LogInRequest.code(), new LogInRequest(new User(157146,"abcde")));

        channel.writeInbound(loginReq);
        assertTrue(sessionMap.checkExists(157146),"用户登录成功后，应该将用户信息存储到 session 中");

        Object response = channel.readOutbound();
        assertTrue(response instanceof DefaultResponseCommand,"用户登录后无论是否成功，都应返回一个的 DefaultResponseCommand 响应");
        assertEquals(((DefaultResponseCommand) response).getAppResponse().getStatus(), ResponseStatus.SUCCESS,
                "响应状态应该是 SUCCESS");
    }

    @Test
    @DisplayName("测试用户登录失败，能够返回登录失败的响应")
    void testLoginFail() {
        EmbeddedChannel channel = newChannelToLogIn();
        BaseRequest logInRequest = new BaseRequest(BizCode.LogInRequest.code(), new LogInRequest(new User(157146,"abcdeXXX")));

        channel.writeInbound(logInRequest);
        assertFalse(sessionMap.checkExists(157146),"用户登录失败后，不应该将用户信息存储到 session 中");

        Object response = channel.readOutbound();
        assertTrue(response instanceof DefaultResponseCommand,"用户登录后无论是否成功，都应返回一个的 DefaultResponseCommand 响应");
        assertEquals(((DefaultResponseCommand) response).getAppResponse().getStatus(), ResponseStatus.FAILED,
                "响应状态应该是 FAILED");
    }

    @Test
    @DisplayName("测试用户登录被挤下线")
    void testLoginKickOff() {
        EmbeddedChannel channel1 = newChannelToLogIn();
        BaseRequest logInRequest = new BaseRequest(BizCode.LogInRequest.code(), new LogInRequest(new User(157146,"abcde")));
        channel1.writeInbound(logInRequest);
        assertTrue(sessionMap.checkExists(157146),"用户登录成功后，应该将用户信息存储到 session 中");
        Session sessionBefore = sessionMap.getSession(157146);

        // 模拟用户重复登录
        EmbeddedChannel channel2 = newChannelToLogIn();
        channel2.writeInbound(logInRequest);
        assertTrue(sessionMap.checkExists(157146),"用户重复登录后，应该将旧的 session 信息删除，并将新的 session 信息存储到 session 中");
        Session sessionAfter = sessionMap.getSession(157146);
        assertNotEquals(sessionBefore,sessionAfter,"两次登录的 session 应该是不同的");


        channel1.readOutbound(); // 第一次 read 结果应该是首次登录的结果
        Object response = channel1.readOutbound();
        assertTrue(response instanceof DefaultRequestCommand,"被挤掉之后，应返回一个的 DefaultRequestCommand 请求，内容是 notice");
        BaseRequest noticeReq = ((DefaultRequestCommand) response).getAppRequest();
        assertTrue(noticeReq.getAppRequest() instanceof Notice, "被挤掉之后，应该返回一个 notice 警告");
    }


    /**
     * 模拟一个 channel，用于测试登录
     */
    private EmbeddedChannel newChannelToLogIn(){
        EmbeddedChannel channel = new EmbeddedChannel();
        CommandHandler mockCommandHandler = Mockito.mock(CommandHandler.class);
        channel.pipeline().addLast(mockCommandHandler);
        try {
            // mock commandHandler 收到请求后会触发 logInProcessor 来处理业务
            Mockito.doAnswer(invocation -> {
                logInProcessor.processRequest(invocation.getArgument(0),1000,invocation.getArgument(1));
                return null;
            }).when(mockCommandHandler).channelRead(Mockito.any(ChannelHandlerContext.class),Mockito.any());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channel;
    }
}