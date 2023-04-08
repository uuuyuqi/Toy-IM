package me.yq.remoting.heartbeat;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import me.yq.remoting.config.Config;
import me.yq.remoting.config.DefaultServerConfig;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.connection.ServerIdleConnHandler;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.session.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ServerIdleConnHandler 测试类，主要覆盖的功能点：
 * 1.客户端空闲超时后，服务端会将其 session 清除
 * @author yq
 * @version v1.0 2023-04-03 16:58
 */
public class ServerIdleConnHandlerTest {

    private final EmbeddedChannel clientChannel = new EmbeddedChannel();

    private final Config serverConfig = new DefaultServerConfig();

    private final SessionMap sessionMap = SessionMap.getInstanceOrCreate(serverConfig);

    private final ServerIdleConnHandler serverIdleConnHandler = new ServerIdleConnHandler(sessionMap);

    private final Session userSession = new Session(157146,clientChannel);



    @BeforeEach
    public void setUp() {
        serverConfig.putConfig(ServerConfigNames.IDLE_CHECK_ENABLE, "true");
        serverConfig.putConfig(ServerConfigNames.CLIENT_TIMEOUT_SECONDS, "5");

        clientChannel.pipeline().addLast(serverIdleConnHandler);

        sessionMap.addSession(userSession);
    }

    @AfterEach
    public void tearDown() {
        clientChannel.close();
    }


    @Test
    @DisplayName("测试客户端超时后，服务端会将对应的 session 摘除")
    void testCleanIdleTimeoutClient() throws InterruptedException {

        assertTrue(sessionMap.checkExists(157146),"检测到 client idle 之前，session 信息应该存在");

        clientChannel.pipeline().fireUserEventTriggered(IdleStateEvent.ALL_IDLE_STATE_EVENT);

        assertFalse(sessionMap.checkExists(157146), "检测到 client idle 之后，session 信息应该被移除");

    }


}
