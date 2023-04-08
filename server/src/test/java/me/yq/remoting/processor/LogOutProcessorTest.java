package me.yq.remoting.processor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.biz.LogOutRequest;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BizCode;
import me.yq.remoting.config.Config;
import me.yq.remoting.config.DefaultServerConfig;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.session.Session;
import me.yq.remoting.transport.process.CommandHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * LogOutProcessor 测试类，主要覆盖的功能点是：
 * 1.用户登出后，会将用户的 channel 信息从 sessionMap 中移除
 * @author yq
 * @version v1.0 2023-04-08 18:52
 */
public class LogOutProcessorTest {

    private final Config config = new DefaultServerConfig();

    private final SessionMap sessionMap = SessionMap.getInstanceOrCreate(config);

    private final LogOutProcessor logOutProcessor = new LogOutProcessor();

    private final CommandHandler mockCommandHandler = Mockito.mock(CommandHandler.class);

    private final EmbeddedChannel channel = new EmbeddedChannel();

    @BeforeEach
    void setUp() throws Exception {
        channel.pipeline().addLast(mockCommandHandler);

        // mock commandHandler 收到请求后会触发 logOutProcessor 来处理业务
        Mockito.doAnswer(invocation -> {
            logOutProcessor.processRequest(invocation.getArgument(0),1000,invocation.getArgument(1));
            return null;
        }).when(mockCommandHandler).channelRead(Mockito.any(ChannelHandlerContext.class),Mockito.any());

        // 事先准备一个在线用户
        sessionMap.addSession(new Session(157146, channel));
    }

    @AfterEach
    void tearDown() {
        sessionMap.removeAllUnSafe();
    }

    @Test
    @DisplayName("测试用户正常登出，能够将用户信息从 session 中移除")
    void testLogoutSuccess() {

        BaseRequest logoutReq = new BaseRequest(BizCode.LogOutRequest.code(), new LogOutRequest(new User(157146)));
        channel.writeInbound(logoutReq);
        assertFalse(sessionMap.checkExists(157146),"用户登出后，应该将用户信息从 session 中移除");
    }

}
