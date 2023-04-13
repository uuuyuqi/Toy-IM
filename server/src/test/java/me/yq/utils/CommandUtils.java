package me.yq.utils;

import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.command.DefaultResponseCommand;

/**
 * RemotingCommand 相关工具类
 * @author yq
 * @version v1.0 2023-04-08 16:10
 */
public class CommandUtils {

    public static DefaultRequestCommand newSerializedSimpleReqCommand(byte bizCode) {
        BaseRequest request = new BaseRequest(bizCode, "test");
        return newSerializedReqCommand(request);
    }


    public static DefaultResponseCommand newSerializedSimpleRespCommand(int reqId) {
        BaseResponse response = new BaseResponse("test");
        return newSerializedRespCommand(reqId,response);
    }


    public static DefaultRequestCommand newSerializedReqCommand(BaseRequest request) {
        DefaultRequestCommand requestCommand = new DefaultRequestCommand();
        requestCommand.setAppRequest(request);
        requestCommand.serialize();
        return requestCommand;
    }

    public static DefaultResponseCommand newSerializedRespCommand(int reqId, BaseResponse response) {
        DefaultResponseCommand responseCommand = new DefaultResponseCommand(reqId);
        responseCommand.setAppResponse(response);
        return responseCommand;
    }



}
