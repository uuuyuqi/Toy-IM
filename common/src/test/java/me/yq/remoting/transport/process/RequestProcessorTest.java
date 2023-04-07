package me.yq.remoting.transport.process;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.BusinessException;
import me.yq.remoting.command.DefaultResponseCommand;
import me.yq.test.processors.SimpleBizProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RequestProcessor 测试类，主要覆盖的功能点是：
 * 1. 收到业务请求之后，可以进行处理并进行业务返回
 * 2. 无论正常处理还是出现异常，都能将处理结果告知给客户端
 * 3. 如果是无需响应的请求，那么就不会有响应
 * @author yq
 * @version v1.0 2023-04-07 17:53
 */
@ExtendWith(MockitoExtension.class)
class RequestProcessorTest {

    private final EmbeddedChannel channel = new EmbeddedChannel();

    @Mock
    private CommandHandler commandHandler;

    private final RequestProcessor simpleBizProcessor = new SimpleBizProcessor();

    private ChannelHandlerContext ctx;

    @BeforeEach
    void setUp() {
        channel.pipeline().addLast(commandHandler);
        ctx = channel.pipeline().context(commandHandler);
    }

    @AfterEach
    void tearDown() {
        channel.close();
    }


    /**
     * 测试处理业务请求并返回
     * @param situationCode 给定的场景码，用于区分不同的场景，1 表示 ok，0 表示失败，-1 表示异常，其余表示单向请求无需返回
     * @param exceptedStatus 预期的响应状态码
     * @param exceptException 预期是否会抛出业务异常
     * @throws Exception BusinessException
     */
    @ParameterizedTest
    @MethodSource("paramsForTestProcessReq")
    @DisplayName("测试处理业务请求并返回")
    void processRequestCommand(int situationCode, ResponseStatus exceptedStatus, boolean exceptException) throws Exception {
        BaseRequest request = new BaseRequest((byte) 127, situationCode);
        int reqId = 12345;
        Mockito.doAnswer(invocation -> {
            simpleBizProcessor.processRequest(ctx, reqId, request);
            return null;
        }).when(commandHandler).channelRead(Mockito.any(), Mockito.any());


        channel.writeInbound(request);
        Object responseCommand = channel.readOutbound();
        if (exceptedStatus == ResponseStatus.OK_NO_NEED_RESPONSE){
            assertNull(responseCommand,"OK_NO_NEED_RESPONSE 类型的处理结果不应该有响应");
            return;
        }
        else
            assertNotNull(responseCommand,"除非无需响应，其他情况下都应该有响应结果");

        assertTrue(responseCommand instanceof DefaultResponseCommand,"响应结果应该是 DefaultResponseCommand 类型");
        BaseResponse response = ((DefaultResponseCommand) responseCommand).getAppResponse();
        assertEquals(exceptedStatus, response.getStatus(),"响应结果的状态码不符合预期");

        if (exceptException)
            assertTrue(response.getAppResponse() instanceof BusinessException,"响应结果应该包含业务异常！");
    }

    /**
     * 提供参数
     */
    static Object[][] paramsForTestProcessReq() {
        return new Object[][]{
                {1, ResponseStatus.SUCCESS, false},
                {0, ResponseStatus.FAILED, false},
                {-1, ResponseStatus.FAILED, true},
                {1000, ResponseStatus.OK_NO_NEED_RESPONSE, false}
        };
    }

}