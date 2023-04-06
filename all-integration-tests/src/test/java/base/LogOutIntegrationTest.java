package base;

import me.yq.common.BizCode;
import me.yq.remoting.config.*;
import me.yq.remoting.processor.LogInProcessor;
import me.yq.remoting.processor.LogOutProcessor;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.support.ChatClient;
import me.yq.support.ChatServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户端登出集成测试，主要覆盖的功能点是：
 * 1.客户端手工发起登出，客户端正常下线，服务端摘除该客户端的信息
 * 2.客户端线程池关闭，客户端异常下线，服务端摘除该客户端的信息
 * @author yq
 * @version v1.0 2023-04-04 09:52
 */
public class LogOutIntegrationTest {
    private final Config serverConfig = new DefaultServerConfig();
    private final Config clientConfig = new DefaultClientConfig();

    private final ChatServer server = new ChatServer(false,serverConfig);
    private final ChatClient client = new ChatClient(false,clientConfig);

    private final ServerSessionMap serverSessionMap = server.getSessionMap();


    @BeforeEach
    public void setUp(){
        serverConfig.putConfig(ServerConfigNames.IDLE_CHECK_ENABLE,"false");
        serverConfig.putConfig(ServerConfigNames.WAIT_RESPONSE_MILLIS,"3600000");
        server.registerBizProcessor(BizCode.LogInRequest,new LogInProcessor(serverSessionMap,serverConfig));
        server.registerBizProcessor(BizCode.LogOutRequest,new LogOutProcessor());
        server.start();

        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE,"false");
        clientConfig.putConfig(ClientConfigNames.WAIT_RESPONSE_MILLIS,"3600000");
        clientConfig.putConfig(ClientConfigNames.SEND_ONEWAY_CONFIRM_MILLIS,"3000");
        client.start();

    }

    @AfterEach
    public void tearDown(){
        server.shutdown();
        client.shutdown();
    }


    @Test
    @DisplayName("测试正常手工登出")
    void test_LogOut(){
        assertDoesNotThrow(()->client.logIn(157146,"abcde"),"登录时不该抛出业务异常");
        assertTrue(client.isOnline(),"登录成功后，客户端应该处于在线状态");
        assertTrue(serverSessionMap.checkExists(157146),"登录成功后，服务端应该能查询到该 session");

        assertDoesNotThrow(()->client.logOut(157146),"注销时不该抛出业务异常");
        assertFalse(client.isOnline(),"注销后，客户端不应该处于在线状态");
        assertFalse(serverSessionMap.checkExists(157146),"注销后，服务端不应该能查询到该 session");
    }


    @Test
    @DisplayName("测试直接关闭客户端线程")
    void test_LogOut_directly(){

        Config newConfig = new DefaultClientConfig();
        newConfig.putConfig(ServerConfigNames.WAIT_RESPONSE_MILLIS,"3600000");
        ChatClient clientToClose = new ChatClient(false,newConfig);
        clientToClose.start();


        assertDoesNotThrow(()->clientToClose.logIn(157146,"abcde"),"登录时不该抛出业务异常");
        assertTrue(clientToClose.isOnline(),"登录成功后，客户端应该处于在线状态");
        assertTrue(serverSessionMap.checkExists(157146),"登录成功后，服务端应该能查询到该 session");

        clientToClose.shutdown();
        assertFalse(clientToClose.isOnline(),"客户端直接关闭后，客户端不应该处于在线状态");
        assertFalse(serverSessionMap.checkExists(157146),"客户端直接关闭后，服务端不应该能查询到该 session");
    }
}
