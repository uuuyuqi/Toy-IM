package me.yq.utils;

import me.yq.common.BaseResponse;
import me.yq.common.ResponseStatus;
import me.yq.remoting.command.DefaultResponseCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 断言工具类，对一些常用的断言进行封装
 *
 * @author yq
 * @version v1.0 2023-04-04 15:43
 */
public class AssertUtils {
    public static void assertResponseStatus(Object toCheck, ResponseStatus status){
        assertNotNull(toCheck,"调用后应该可以得到非空的返回对象");
        assertTrue( toCheck instanceof DefaultResponseCommand,"返回对象应该是 DefaultResponseCommand 通信包装对象");
        DefaultResponseCommand responseCommand = (DefaultResponseCommand) toCheck;
        responseCommand.deserialize();
        BaseResponse response = responseCommand.getAppResponse();
        assertEquals(status,response.getStatus(),"返回状态应该是 " + status);
    }
}
