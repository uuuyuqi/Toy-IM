package me.yq.remoting.connection;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import me.yq.remoting.command.HeartbeatCommand;
import me.yq.remoting.config.ClientConfigNames;
import me.yq.remoting.config.Config;
import me.yq.support.ChatClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClientHeartbeatHandler 测试类，主要覆盖的功能点：
 * 1. 每当 channel 中传来空闲事件，就能接收到该事件并尝试发出心跳
 * 2. 当心跳处理器检测到心跳失败次数超过最大值时，会通知客户端，由客户端发起下一步行为
 */
@ExtendWith(MockitoExtension.class)
class ClientHeartbeatHandlerTest {

    private final int idleSeconds = 1;

    private final AtomicInteger heartbeatCount = new AtomicInteger(0);

    private final EmbeddedChannel channel = new EmbeddedChannel();

    private final ChatClient clientMock = Mockito.mock(ChatClient.class);

    private final ClientHeartbeatHandler heartbeatHandler = new ClientHeartbeatHandler(clientMock);

    private final ClientHeartbeatHandler spy = Mockito.spy(heartbeatHandler);


    @BeforeEach
    void setUp() {
        channel.pipeline().addLast(spy);
    }

    @AfterEach
    void tearDown() {
        heartbeatCount.set(0);
        channel.close();
    }


    @Test
    @DisplayName("测试每隔固定空闲时间会发出心跳")
    void sendHeartbeat() throws Exception {

        int exceptCount = 3;

        // 配置 心跳最大失败次数 为足够大的数值
        Mockito.when(clientMock.getConfig()).thenReturn(new Config() {
            {
                putConfig(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT,"65536");
            }
        });

        // 每当心跳被触发，就计数 + 1
        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            // 检测是否有心跳数据被发送出去
            heartbeatCount.incrementAndGet();
            Object o = channel.readOutbound();
            assertNotNull(o,"channel 应该能读取出对象");
            assertTrue(o instanceof HeartbeatCommand,"发送的数据应该是心跳对象");

            return null;
        }).when(spy).userEventTriggered(Mockito.any(), Mockito.any());

        // 模拟向 channel 中发送 exceptCount 次空闲事件
        for (int i = 0; i < exceptCount; i++) {
            channel.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);
            TimeUnit.SECONDS.sleep(idleSeconds);
        }

        assertEquals(exceptCount, heartbeatCount.get(), "心跳次数应该是: " + exceptCount);
    }


    @Test
    @DisplayName("测试最大心跳次数后告知客户端")
    void noticeAfterMaxCountBeat() throws Exception {

        // 配置 心跳最大失败次数 为 3 次
        int maxHeartbeatFailCount = 3;

        Mockito.when(clientMock.getConfig()).thenReturn(new Config() {
            {
                putConfig(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT,"3");
            }
        });

        // 每当心跳被触发，就计数 + 1
        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            // 检测是否有心跳数据被发送出去
            heartbeatCount.incrementAndGet();
            Object o = channel.readOutbound();
            assertNotNull(o,"channel 应该能读取出对象");
            assertTrue(o instanceof HeartbeatCommand,"发送的数据应该是心跳对象");

            return null;
        }).when(spy).userEventTriggered(Mockito.any(), Mockito.any());

        // 模拟向 channel 中发送 （最大心跳失败数+1） 次空闲事件
        // 确保心跳失败
        for (int i = 0; i < maxHeartbeatFailCount + 1; i++) {
            channel.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);
            TimeUnit.SECONDS.sleep(idleSeconds);
        }

        // 检测是否告知客户端
        Mockito.verify(clientMock).loseConnection();
    }


}