package base;

import me.yq.biz.domain.User;
import me.yq.common.BizCode;
import me.yq.remoting.config.*;
import me.yq.remoting.processor.LogInProcessor;
import me.yq.remoting.processor.MessageReceivedProcessor;
import me.yq.remoting.processor.MessagingTransferProcessor;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.utils.CommonUtils;
import me.yq.support.ChatClient;
import me.yq.support.ChatServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端消息转发集成测试，主要覆盖的功能点是：
 * 1.客户端 A 能成功发送消息
 * 2.客户端 B 能成功接收到客户端 A 发送的消息
 * 3.服务端能成功转发消息
 * @author yq
 * @version v1.0 2023-04-04 09:53
 */
public class MessageTransferIntegrationTest {
    private final Config serverConfig = new DefaultServerConfig();
    private final ChatServer server = new ChatServer(false, serverConfig);
    private final ServerSessionMap serverSessionMap = server.getSessionMap();

    private final Config clientAConfig = new DefaultClientConfig();
    private final Config clientBConfig = new DefaultClientConfig();
    private final ChatClient spyClientA = Mockito.spy(new ChatClient(false, clientAConfig));
    private final ChatClient spyClientB = Mockito.spy(new ChatClient(false, clientBConfig));

    private final User userA = new User(157146,"abcde");
    private final User userB = new User(909900,"123456");



    @BeforeEach
    public void setUp(){
        // 准备服务端
        serverConfig.putConfig(ServerConfigNames.IDLE_CHECK_ENABLE,"false");
        server.registerBizProcessor(BizCode.LogInRequest,new LogInProcessor(serverSessionMap, serverConfig));
        server.registerBizProcessor(BizCode.Messaging,new MessagingTransferProcessor(serverSessionMap, serverConfig));
        server.start();

        // 准备客户端
        clientAConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE,"false");
        spyClientA.registerBizProcessor(BizCode.Messaging,new MessageReceivedProcessor(spyClientA));
        spyClientA.start();

        clientBConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE,"false");
        spyClientB.registerBizProcessor(BizCode.Messaging,new MessageReceivedProcessor(spyClientB));
        spyClientB.start();

        // 定义 spy 客户端的行为，转交给真正服务端执行


        // 让两个客户端先登录并连接到服务端
        assertDoesNotThrow(()->spyClientA.logIn(userA.getUserId(),userA.getPasswd()),"客户端 A 登录不该抛出异常");
        assertTrue(serverSessionMap.checkExists(userA.getUserId()),"客户端 A 登录失败");
        assertDoesNotThrow(()->spyClientB.logIn(userB.getUserId(),userB.getPasswd()),"客户端 B 登录不该抛出异常");

        ;
        assertTrue(serverSessionMap.checkExists(userB.getUserId()),"客户端 B 登录失败");

    }

    @AfterEach
    public void tearDown(){
        spyClientA.shutdown();
        spyClientB.shutdown();

        server.shutdown();
    }

    @Test
    @DisplayName("测试客户端发送消息")
    void test_sendMessage(){
        int messageCount = 10;
        assertDoesNotThrow(()->{
            for (int i = 0; i < messageCount; i++) {
                spyClientA.sendMsg(909900,"hello:" + CommonUtils.now());
                spyClientB.sendMsg(157146,"hello:" + CommonUtils.now());
            }
        },"消息互发时，不应该抛出异常");

        Mockito.verify(spyClientA,Mockito.times(messageCount)).acceptMsg(Mockito.any(User.class),Mockito.anyString());
        Mockito.verify(spyClientB,Mockito.times(messageCount)).acceptMsg(Mockito.any(User.class),Mockito.anyString());
    }
}
