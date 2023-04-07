package me.yq.remoting.codec.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.remoting.command.DefaultRequestCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProtocolCodec 测试类。主要覆盖的功能：
 * 1.编码：将 RemotingCommand 编码为 ByteBuf
 * 2.解码：将 ByteBuf 解码为 RemotingCommand
 * 3.连续发送多个包，能够正确解码
 * 4.发生了半包，能够正确解码
 * 5.发生了粘包，能够正确解码
 * 6.发生了粘包和半包混合的情况，能够正确解码
 */
class ProtocolCodecTest {

    private EmbeddedChannel clientChannel;
    private EmbeddedChannel serverChannel;

    private final String testContent = "hello yq! 乱码来喽～ @#\\\"<>?¥$ ȞÍ✶Ⓗ₷〄☞❅✿" + (char) 1 + (char) 15;

    @BeforeEach
    void prepareChannel() {
        clientChannel = new EmbeddedChannel();
        clientChannel.pipeline().addLast(new ProtocolCodec());

        serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(new ProtocolCodec());
    }

    @AfterEach
    void cleanUp() {
        if (clientChannel != null && serverChannel != null) {
            clientChannel.close();
            serverChannel.close();
        }
    }


    @Test
    @DisplayName("测试正常使用")
    void test_normal_use() {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes(testContent.getBytes(StandardCharsets.UTF_8));

        // do test encode  decode
        clientChannel.writeOutbound(requestCommand);
        assertDoesNotThrow(clientChannel::checkException,"编码后不应该有异常");
        Object sendByClient = clientChannel.outboundMessages().poll();

        serverChannel.writeInbound(sendByClient);
        assertDoesNotThrow(clientChannel::checkException,"解码后不应该有异常");
        Object receivedByServer = serverChannel.inboundMessages().poll();
        assertNotNull(receivedByServer, "解码后不应该为空");
        String result = new String(((DefaultRequestCommand) receivedByServer).getContentBytes(), StandardCharsets.UTF_8);

        assertEquals(testContent, result, "解码后的内容应该和编码前的内容一致");
    }

    @Test
    @DisplayName("测试连续发送 100 个包")
    void test_multi_package_send() {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes(testContent.getBytes(StandardCharsets.UTF_8));

        // do test
        for (int i = 0; i < 100; i++) {
            clientChannel.writeOneOutbound(requestCommand);
        }
        clientChannel.flushOutbound();

        assertDoesNotThrow(clientChannel::checkException,"编码后不应该有异常");
        Object[] sendByClient = clientChannel.outboundMessages().toArray();

        serverChannel.writeInbound(sendByClient);
        assertDoesNotThrow(clientChannel::checkException,"解码后不应该有异常");
        Object[] receivedByServer = serverChannel.inboundMessages().toArray();

        assertEquals(100, receivedByServer.length);

        for (Object o : receivedByServer) {
            String result = new String(((DefaultRequestCommand) o).getContentBytes(), StandardCharsets.UTF_8);
            assertEquals(testContent, result, "解码后的内容应该和编码前的内容一致");
        }

    }


    @Test
    @DisplayName("测试半包")
    void test_half_package() {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes(testContent.getBytes(StandardCharsets.UTF_8));

        // do test encode  decode
        clientChannel.writeOutbound(requestCommand);
        assertDoesNotThrow(clientChannel::checkException,"编码后不应该有异常");
        Object sendByClient = clientChannel.outboundMessages().poll();

        ByteBuf rawBuf = (ByteBuf) sendByClient;
        int len = rawBuf.readableBytes();
        ByteBuf slice1 = rawBuf.slice(0, len / 2);
        // 防止半包的时候切片被释放，这里加一下计数
        slice1.retain();
        ByteBuf slice2 = rawBuf.slice(len / 2, len - len / 2);

        serverChannel.writeInbound(slice1);
        serverChannel.writeInbound(slice2);
        assertDoesNotThrow(clientChannel::checkException,"解码后不应该有异常");
        Object receivedByServer = serverChannel.inboundMessages().poll();
        assertNotNull(receivedByServer, "解码后不应该为空");
        String result = new String(((DefaultRequestCommand) receivedByServer).getContentBytes(), StandardCharsets.UTF_8);

        assertEquals(testContent, result, "解码后的内容应该和编码前的内容一致");
    }


    @Test
    @DisplayName("测试粘包")
    void test_stick_package() {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes(testContent.getBytes(StandardCharsets.UTF_8));

        // send
        for (int i = 0; i < 100; i++) {
            clientChannel.writeOneOutbound(requestCommand);
        }
        clientChannel.flushOutbound();
        assertDoesNotThrow(clientChannel::checkException,"编码后不应该有异常");

        // collect
        Object[] sendByClient = clientChannel.outboundMessages().toArray();

        // assembly
        ByteBuf stickBuf = ByteBufAllocator.DEFAULT.buffer();
        for (Object byteBuf : sendByClient) {
            stickBuf.writeBytes((ByteBuf) byteBuf);
        }

        // send
        serverChannel.writeInbound(stickBuf);
        assertDoesNotThrow(clientChannel::checkException);
        Object[] receivedByServer = serverChannel.inboundMessages().toArray();

        assertEquals(100, receivedByServer.length,"应该收到 100 个包");

        // check
        for (Object o : receivedByServer) {
            String result = new String(((DefaultRequestCommand) o).getContentBytes(), StandardCharsets.UTF_8);
            assertEquals(testContent, result, "解码后的内容应该和编码前的内容一致");
        }

    }


    // 测出 decode bug：
    // decode 出 header、content 时，应该考虑 header 和 content 长度标记也要额外加 8 字节( 2个 int)
    @Test
    @DisplayName("测试粘包半包混合")
    void test_stick_half_stick_package() {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setContentBytes(testContent.getBytes(StandardCharsets.UTF_8));

        // send
        for (int i = 0; i < 100; i++) {
            clientChannel.writeOneOutbound(requestCommand);
        }
        clientChannel.flushOutbound();
        assertDoesNotThrow(clientChannel::checkException,"编码后不应该有异常");

        // collect
        Object[] sendByClient = clientChannel.outboundMessages().toArray();

        // assembly
        ByteBuf stickBuf = ByteBufAllocator.DEFAULT.buffer();
        for (Object byteBuf : sendByClient) {
            stickBuf.writeBytes((ByteBuf) byteBuf);
        }

        // slice
        int len = stickBuf.readableBytes();
        ByteBuf slice1 = stickBuf.slice(0, len / 3);
        slice1.retain();
        ByteBuf slice2 = stickBuf.slice(len / 3, len / 15);
        slice2.retain();
        ByteBuf slice3 = stickBuf.slice(len / 3 + len / 15, len - len / 3 - len / 15);
        slice3.retain();


        // send
        serverChannel.writeInbound(slice1);
        serverChannel.writeInbound(slice2);
        serverChannel.writeInbound(slice3);
        assertDoesNotThrow(clientChannel::checkException,"解码后不应该有异常");
        Object[] receivedByServer = serverChannel.inboundMessages().toArray();

        // check
        for (Object o : receivedByServer) {
            String result = new String(((DefaultRequestCommand) o).getContentBytes(), StandardCharsets.UTF_8);
            assertEquals(testContent, result, "解码后的内容应该和编码前的内容一致");
        }

    }
}
