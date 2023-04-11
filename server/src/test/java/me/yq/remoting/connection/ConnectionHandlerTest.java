package me.yq.remoting.connection;

import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.remoting.config.DefaultServerConfig;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.Config;
import me.yq.remoting.transport.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConnectionHandler 测试类，主要覆盖的功能：
 * 1.IM 连接发生断开，能够从 session 中进行清理
 * @author yq
 * @version v1.0 2023-04-08 13:42
 */
class ConnectionHandlerTest {

    private final EmbeddedChannel clientChannel = new EmbeddedChannel();

    private final Config config = new DefaultServerConfig();

    private final SessionMap sessionMap = SessionMap.getInstanceOrCreate(config);

    private final ConnectionHandler connectionHandler = new ConnectionHandler(sessionMap);

    private final Session userSession = new Session(157146,clientChannel);

    @BeforeEach
    void setUp() {
        clientChannel.pipeline().addLast(connectionHandler);
        sessionMap.addSession(userSession);
    }

    @AfterEach
    void tearDown() {
        clientChannel.close();
    }

    @Test
    @DisplayName("测试 IM 连接断开对 session 信息的移除")
    void testChannelInactive() {
        assertTrue(sessionMap.checkExists(157146),"传播 channelInactive 事件前，session 信息应该存在");

        clientChannel.pipeline().fireChannelInactive();

        assertFalse(sessionMap.checkExists(157146),"传播 channelInactive 事件后，session 信息应该被移除");
    }
}