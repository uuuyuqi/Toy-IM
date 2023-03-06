package me.yq.remoting.codec.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.remoting.transport.command.DefaultRequestCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 编解码测试：测了普通使用、粘包、半包、及其混合出现的情况
 *
 */
class ProtocolCodecTest {

    private EmbeddedChannel clientChannel;
    private EmbeddedChannel serverChannel;

    @BeforeEach
    void prepareChannel(){
        // init client channel
        clientChannel = new EmbeddedChannel();
        clientChannel.pipeline().addLast(new ProtocolCodec());

        // init server channel
        serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(new ProtocolCodec());
    }

    @AfterEach
    void cleanUp(){
        if (clientChannel!=null && serverChannel!=null){
            clientChannel.close();
            serverChannel.close();
        }
    }


    @Test
    @DisplayName("测试正常使用")
    void test_normal_use(){
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes("hello yq yq yq yq yq yq yq haha! this is a test".getBytes(StandardCharsets.UTF_8));

        // do test encode  decode
        clientChannel.writeOutbound(requestCommand);
        assertDoesNotThrow(clientChannel:: checkException);
        Object sendByClient = clientChannel.outboundMessages().poll();

        serverChannel.writeInbound(sendByClient);
        assertDoesNotThrow(clientChannel:: checkException);
        Object receivedByServer = serverChannel.inboundMessages().poll();
        String result = new String(((DefaultRequestCommand)receivedByServer).getContentBytes(),StandardCharsets.UTF_8);

        assertEquals("hello yq yq yq yq yq yq yq haha! this is a test", result);
    }

    @Test
    @DisplayName("测试连续发送 100 个包")
    void test_multi_package_send(){
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes("hello yq yq yq yq yq yq yq haha! this is a test".getBytes(StandardCharsets.UTF_8));

        // do test
        for (int i = 0; i < 100; i++) {
            clientChannel.writeOneOutbound(requestCommand);
        }
        clientChannel.flushOutbound();

        assertDoesNotThrow(clientChannel:: checkException);
        Object[] sendByClient = clientChannel.outboundMessages().toArray();

        serverChannel.writeInbound(sendByClient);
        assertDoesNotThrow(clientChannel:: checkException);
        Object[] receivedByServer = serverChannel.inboundMessages().toArray();

        assertEquals(100,receivedByServer.length);

        for (Object o : receivedByServer) {
            String result = new String(((DefaultRequestCommand)o).getContentBytes(),StandardCharsets.UTF_8);
            assertEquals("hello yq yq yq yq yq yq yq haha! this is a test", result);
        }

    }


    @Test
    @DisplayName("测试半包")
    void test_half_package(){
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes("hello yq yq yq yq yq yq yq haha! this is a test".getBytes(StandardCharsets.UTF_8));

        // do test encode  decode
        clientChannel.writeOutbound(requestCommand);
        assertDoesNotThrow(clientChannel:: checkException);
        Object sendByClient = clientChannel.outboundMessages().poll();

        ByteBuf  rawBuf = (ByteBuf) sendByClient;
        int len = rawBuf.readableBytes();
        ByteBuf slice1 = rawBuf.slice(0, len/2);
        // 防止半包的时候切片被释放，这里加一下计数
        slice1.retain();
        ByteBuf slice2 = rawBuf.slice(len/2,len-len/2);

        serverChannel.writeInbound(slice1);
        serverChannel.writeInbound(slice2);
        assertDoesNotThrow(clientChannel:: checkException);
        Object receivedByServer = serverChannel.inboundMessages().poll();
        String result = new String(((DefaultRequestCommand)receivedByServer).getContentBytes(),StandardCharsets.UTF_8);

        assertEquals("hello yq yq yq yq yq yq yq haha! this is a test", result);
    }


    @Test
    @DisplayName("测试粘包")
    void test_stick_package(){
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes("hello yq yq yq yq yq yq yq haha! this is a test".getBytes(StandardCharsets.UTF_8));

        // send
        for (int i = 0; i < 100; i++) {
            clientChannel.writeOneOutbound(requestCommand);
        }
        clientChannel.flushOutbound();
        assertDoesNotThrow(clientChannel:: checkException);

        // collect
        Object[] sendByClient = clientChannel.outboundMessages().toArray();

        // assembly
        ByteBuf stickBuf = ByteBufAllocator.DEFAULT.buffer();
        for (Object byteBuf : sendByClient) {
            stickBuf.writeBytes((ByteBuf) byteBuf);
        }

        // send
        serverChannel.writeInbound(stickBuf);
        assertDoesNotThrow(clientChannel:: checkException);
        Object[] receivedByServer = serverChannel.inboundMessages().toArray();

        assertEquals(100,receivedByServer.length);

        // check
        for (Object o : receivedByServer) {
            String result = new String(((DefaultRequestCommand)o).getContentBytes(),StandardCharsets.UTF_8);
            assertEquals("hello yq yq yq yq yq yq yq haha! this is a test", result);
        }

    }



    // 测出 decode bug：
    // decode 出 header、content 时，应该考虑 header 和 content 长度标记也要额外加 8 字节( 2个 int)
    @Test
    @DisplayName("测试粘包半包混合")
    void test_stick_half_stick_package(){
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes("hello yq yq yq yq yq yq yq haha! this is a test".getBytes(StandardCharsets.UTF_8));

        // send
        for (int i = 0; i < 100; i++) {
            clientChannel.writeOneOutbound(requestCommand);
        }
        clientChannel.flushOutbound();
        assertDoesNotThrow(clientChannel:: checkException);

        // collect
        Object[] sendByClient = clientChannel.outboundMessages().toArray();

        // assembly
        ByteBuf stickBuf = ByteBufAllocator.DEFAULT.buffer();
        for (Object byteBuf : sendByClient) {
            stickBuf.writeBytes((ByteBuf) byteBuf);
        }

        // slice
        int len = stickBuf.readableBytes();
        ByteBuf slice1 = stickBuf.slice(0, len/3);
        slice1.retain();
        ByteBuf slice2 = stickBuf.slice(len/3,len/15);
        slice2.retain();
        ByteBuf slice3 = stickBuf.slice(len/3+len/15,len-len/3-len/15);
        slice3.retain();


        // send
        serverChannel.writeInbound(slice1);
        serverChannel.writeInbound(slice2);
        serverChannel.writeInbound(slice3);
        assertDoesNotThrow(clientChannel:: checkException);
        Object[] receivedByServer = serverChannel.inboundMessages().toArray();

        // check
        for (Object o : receivedByServer) {
            String result = new String(((DefaultRequestCommand)o).getContentBytes(),StandardCharsets.UTF_8);
            assertEquals("hello yq yq yq yq yq yq yq haha! this is a test", result);
        }

    }
}
