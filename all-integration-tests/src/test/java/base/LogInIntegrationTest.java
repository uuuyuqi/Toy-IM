package base;

import me.yq.biz.Notice;
import me.yq.biz.domain.User;
import me.yq.common.BizCode;
import me.yq.common.exception.BusinessException;
import me.yq.remoting.config.*;
import me.yq.remoting.processor.LogInProcessor;
import me.yq.remoting.processor.NoticeFromServerProcessor;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.support.session.Session;
import me.yq.support.ChatClient;
import me.yq.support.ChatServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 客户端登录集成测试，主要覆盖的功能点是：
 * 1.客户端输入正确账号密码时，可以正常登录
 * 2.客户端正常登录后，可以获取到当前账户的基本信息
 * 3.客户端输入错误的账号或密码时，提示用户名或密码错误
 * 4.客户端登录之后，可以被其他登录挤掉线
 * 5.当发送挤掉线行为时，服务端会推送安全警告，客户端会收到安全警告
 * @author yq
 * @version v1.0 2023-04-04 09:52
 */
@ExtendWith(MockitoExtension.class)
public class LogInIntegrationTest {

    private final Config serverConfig = new DefaultServerConfig();
    private final Config clientConfig = new DefaultClientConfig();

    private final ChatServer server = new ChatServer(false,serverConfig);
    // spy 的方式测试 功能点5 是否收到安全警告
    private final ChatClient spyClient = Mockito.spy(new ChatClient(false,clientConfig));

    private final ServerSessionMap serverSessionMap = server.getSessionMap();

    @BeforeEach
    public void setUp(){
        serverConfig.putConfig(ServerConfigNames.IDLE_CHECK_ENABLE,"false");
        server.registerBizProcessor(BizCode.LogInRequest,new LogInProcessor(serverSessionMap,serverConfig));
        server.start();

        clientConfig.putConfig(ClientConfigNames.HEARTBEAT_ENABLE,"false");
        spyClient.registerBizProcessor(BizCode.Noticing, new NoticeFromServerProcessor(spyClient));
        spyClient.start();
    }

    @AfterEach
    public void tearDown(){
        server.shutdown();
        spyClient.shutdown();
    }



    @Test
    @DisplayName("测试登录成功")
    void testLogIn_success(){
        assertDoesNotThrow(()->{
            spyClient.logIn(157146,"abcde");
        },"应该登录成功，不应该抛出任何异常");
        assertTrue(serverSessionMap.checkExists(157146),"服务端应该保存登录成功的 session");
        assertTrue(spyClient.isOnline(),"客户端应该处于在线状态");
    }



    @Test
    @DisplayName("测试登录成功后，能获取到 user 基本信息")
    void testLogIn_success_getUserInfo(){
        assertDoesNotThrow(()->spyClient.logIn(157146,"abcde"),"登录时不该抛出业务异常");
        assertTrue(serverSessionMap.checkExists(157146),"登录失败");


        User currentUser = spyClient.getCurrentUser();
        // 检测所有的用户信息是否都获取到了
        assertNotNull(currentUser,"获取到用户信息为空");
        assertNotNull(currentUser.getName(),"获取到用户 name 为空");
        assertNotNull(currentUser.getAddress(),"获取到用户 address 为空");
        assertNotNull(currentUser.getSignature(),"获取到用户 signature 为空");

    }




    @Test
    @DisplayName("测试密码错误，登录失败且抛业务异常")
    void testLogIn_fail(){

        // 客户端抛异常
        BusinessException bizException = assertThrows(BusinessException.class, () -> {
            spyClient.logIn(157146, "abcdex");
        },"登录失败，应该抛出异常");
        assertTrue(bizException.getMessage().contains("用户名或密码错误"),"异常应该包含“用户名或密码错误”");
        assertFalse(spyClient.isOnline(),"客户端不应该处于在线状态");

        // 服务端不保存 session
        assertFalse(serverSessionMap.checkExists(157146),"服务端不应该保存登录失败的 session");
    }



    @Test
    @DisplayName("测试挤掉线，并且能够收到安全警告")
    void testLogIn_EdgeOut(){

        assertDoesNotThrow(()->spyClient.logIn(157146,"abcde"),"登录时不该抛出业务异常");

        Session session1 = serverSessionMap.getSession(157146);
        assertNotNull(session1,"原端登录成功");
        assertTrue(spyClient.isOnline(),"原端客户端应该处于在线状态");

        ChatClient clientNew = new ChatClient(false);
        clientNew.registerBizProcessor(BizCode.Noticing, new NoticeFromServerProcessor(clientNew));
        clientNew.start();
        clientNew.logIn(157146,"abcde");
        Session session2 = serverSessionMap.getSession(157146);
        assertNotNull(session2,"另一端登录成功");
        assertTrue(clientNew.isOnline(),"另一端客户端应该处于在线状态");

        assertNotEquals(session1,session2,"服务端 session 应该发生了变化");

        Mockito.verify(spyClient).acceptNotice(Mockito.any(Notice.class));
        clientNew.shutdown();
    }
}
