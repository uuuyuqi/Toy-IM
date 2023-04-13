package base;

import common.BlockAllHandlerSupplier;
import common.TestConfigNames;
import io.netty.channel.ChannelHandler;
import me.yq.common.BizCode;
import me.yq.remoting.config.ClientConfigNames;
import me.yq.remoting.config.DefaultClientConfig;
import me.yq.remoting.config.DefaultServerConfig;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.processor.LogInProcessor;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.support.Config;
import me.yq.support.ChatClient;
import me.yq.support.ChatServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户端心跳保活集成测试，主要覆盖的功能点时：
 * 1. 客户端能够正常发出心跳，并计算心跳次数和自动下线
 * 2. 客户端能够正常处理服务端的心跳 ack
 * 3. 服务端能够正常返回心跳 ack，客户端接收到正常 ack 响应后，会清空心跳次数
 * 4. 服务端发现客户端长时间无数据来往，会自动将其下线并摘除 session
 * @author yq
 * @version v1.0 2023-04-04 09:52
 */
public class ClientServerConnKeepAliveIntegrationTest {
    private final Config serverConfig = new DefaultServerConfig();
    private final Config clientConfig = new DefaultClientConfig();

    private final ChatServer server = new ChatServer(false,serverConfig);
    private final ChatClient client = new ChatClient(false,clientConfig);

    private final SessionMap sessionMap = server.getSessionMap();

    /**
     * 可以阻塞一切 IO 请求的 handler，用于模拟服务端阻塞的情况
     */
    private final Supplier<ChannelHandler> blockerHandlerSupplier = new BlockAllHandlerSupplier(serverConfig);

    @BeforeEach
    public void setUp(){
        serverConfig.putConfig(ServerConfigNames.CLIENT_TIMEOUT_SECONDS,"5");
        // 这个是自定义配置，主要用于本次测试。用于控制是否开关全局阻塞
        // 一旦打开，所有的 channel 都无法接收任何请求，整个服务端如同 hang 住一样
        serverConfig.putConfig(TestConfigNames.BLOCK_ALL_IO_THREADS,"false");
        server.registerBizProcessor(BizCode.LogInRequest.code(),new LogInProcessor(sessionMap,serverConfig));

        server.registerCustomHandlersAhead("blockAllHandler", blockerHandlerSupplier);

        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_IDLE_SECONDS,"2");
    }

    @AfterEach
    public void tearDown(){
        server.shutdown();
        client.shutdown();
    }


    @Test
    @DisplayName("测试客户端能够正常发出心跳，并计算心跳次数和主观下线")
    public void testHeartbeat_client_keep_sending() throws InterruptedException {

        startWithDiffIdleConfig(false,true);

        assertDoesNotThrow(() -> client.logIn(157146, "abcde"),"登录时不该抛出业务异常");
        assertTrue(sessionMap.checkExists(157146));
        assertTrue(client.isOnline(),"客户端应该处于在线状态");

        // 开启全局阻塞，模拟服务端 hang 住
        serverConfig.putConfig(TestConfigNames.BLOCK_ALL_IO_THREADS,"true");


        int beatIntervalSec = clientConfig.getInt(ClientConfigNames.HEARTBEAT_IDLE_SECONDS);
        int maxHeartbeatCount = clientConfig.getInt(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT);

        TimeUnit.SECONDS.sleep((long) beatIntervalSec * (maxHeartbeatCount * 2L));

        // 超出 max 次心跳之后: 1.客户端会发出至少 max 次心跳 2.客户端主动业务下线，不可继续聊天
        assertTrue(
                client.getServerSession().getChannel().attr(ChannelAttributes.HEARTBEAT_COUNT).get() >= maxHeartbeatCount,
                "此时客户端心跳次数应该大于等于最大心跳次数");
        assertFalse(client.isOnline(),"客户端应该处于离线状态");

    }

    @Test
    @DisplayName("服务端能够正常返回心跳 ack")
    public void testHeartbeat_server_ack() throws InterruptedException {
        startWithDiffIdleConfig(false,true);


        assertDoesNotThrow(() -> client.logIn(157146, "abcde"),"登录时不该抛出业务异常");
        assertTrue(sessionMap.checkExists(157146));
        assertTrue(client.isOnline(),"客户端应该处于在线状态");


        int beatIntervalSec = clientConfig.getInt(ClientConfigNames.HEARTBEAT_IDLE_SECONDS);
        int maxHeartbeatCount = clientConfig.getInt(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT);


        // 超出 max 次心跳之后: 1.客户端会发出至少 max 次心跳 2.客户端主动业务下线，不可继续聊天
        TimeUnit.SECONDS.sleep((long) beatIntervalSec * (maxHeartbeatCount * 2L));
        assertTrue(
                client.getServerSession().getChannel().attr(ChannelAttributes.HEARTBEAT_COUNT).get() <= 1 ,
                "此时会发生心跳，但是由于服务端不断响应 ack，心跳计数应该不超过 1");
        assertTrue(client.isOnline(),"客户端应该处于离线状态");
        assertTrue(sessionMap.checkExists(157146),"服务端会话应该还存在");
    }



    @Test
    @DisplayName("客户端超时后，服务端能够检测出并清理会话")
    public void testHeartbeat_server_clean_session() throws InterruptedException {

        startWithDiffIdleConfig(true, false);

        assertDoesNotThrow(() -> client.logIn(157146, "abcde"),"登录时不该抛出业务异常");
        assertTrue(sessionMap.checkExists(157146));
        assertTrue(client.isOnline(), "客户端应该处于在线状态");

        TimeUnit.SECONDS.sleep(serverConfig.getLong(ServerConfigNames.CLIENT_TIMEOUT_SECONDS) + 1);

        assertFalse(sessionMap.checkExists(157146),"服务端会话应该被清理");
        assertFalse(client.isOnline(),"客户端应该处于离线状态");
    }



    /**
     * 启动服务端和客户端，可以制定是否空闲检测
     * @param serverIdleCheckEnable 服务端是否开启空闲检测
     * @param clientHeartbeatEnable 客户端是否开启心跳
     */
    private void startWithDiffIdleConfig(boolean serverIdleCheckEnable, boolean clientHeartbeatEnable) {
        serverConfig.putConfig(ServerConfigNames.IDLE_CHECK_ENABLE,String.valueOf(serverIdleCheckEnable));
        server.start();

        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE,String.valueOf(clientHeartbeatEnable));
        client.start();
    }

}
