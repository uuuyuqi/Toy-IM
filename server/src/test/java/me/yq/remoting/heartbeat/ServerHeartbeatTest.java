package me.yq.remoting.heartbeat;

import me.yq.common.BizCode;
import me.yq.remoting.config.*;
import me.yq.remoting.processor.LogInProcessor;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.support.ChatClient;
import me.yq.support.ChatServer;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

/**
 * @author yq
 * @version v1.0 2023-04-03 16:58
 */
public class ServerHeartbeatTest {


    private final Config serverConfig = new DefaultServerConfig();

    private final ChatServer server = new ChatServer(false, serverConfig);

    private final ServerSessionMap serverSessionMap = server.getSessionMap();


    @BeforeEach
    public void setUp() {
        serverConfig.putConfig(ServerConfigNames.IDLE_CHECK_ENABLE, "true");
        serverConfig.putConfig(ServerConfigNames.CLIENT_TIMEOUT_SECONDS, "5");
        server.registerBizProcessor(BizCode.LogInRequest, new LogInProcessor(serverSessionMap, serverConfig));
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.shutdown();
    }


    @Test
    @DisplayName("测试服务端正常响应心跳")
    void testServerHeartbeat() throws InterruptedException {

        // 客户端开启心跳
        Config clientConfig = new DefaultClientConfig();
        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE, "true");
        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_IDLE_SECONDS, "3");
        ChatClient client = new ChatClient(false, clientConfig);
        client.start();

        try {
            client.logIn(157146, "abcde");
            Assertions.assertTrue(serverSessionMap.checkExists(157146));
            Assertions.assertTrue(client.isOnline());

            long beatIntervalSec = clientConfig.getInt(ClientConfigNames.HEARTBEAT_IDLE_SECONDS);
            int maxHeartbeatCount = clientConfig.getInt(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT);

            TimeUnit.SECONDS.sleep(beatIntervalSec * maxHeartbeatCount);
            Assertions.assertTrue(client.isOnline());

        } catch (Exception e) {
            Assertions.fail(e);

        } finally {
            client.shutdown();

        }
    }


    @Test
    @DisplayName("测试客户端不响应心跳，服务端主动清除连接")
    void testServerHeartbeat_no_response() throws InterruptedException {
        // 客户端关闭心跳
        Config clientConfig = new DefaultClientConfig();
        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE, "false");
        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_IDLE_SECONDS, "3");
        ChatClient client = new ChatClient(false, clientConfig);
        client.start();

        try {
            client.logIn(157146, "abcde");
            Assertions.assertTrue(serverSessionMap.checkExists(157146));
            Assertions.assertTrue(client.isOnline());

            long intervalBeforeClose = serverConfig.getLong(ServerConfigNames.CLIENT_TIMEOUT_SECONDS);

            TimeUnit.SECONDS.sleep(intervalBeforeClose + 3);  // 冗余 3 秒留给服务端执行和整理
            Assertions.assertFalse(serverSessionMap.checkExists(157146));

        } catch (Exception e) {
            Assertions.fail(e);

        } finally {
            client.shutdown();

        }
    }
}
