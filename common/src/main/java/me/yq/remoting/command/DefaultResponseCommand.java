package me.yq.remoting.command;

import me.yq.common.BaseResponse;
import me.yq.remoting.transport.serializer.Serializer;
import me.yq.remoting.transport.serializer.SerializerFactory;

/**
 * 基础通信响应对象
 * @author yq
 * @version v1.0 2023-02-21 09:27
 */
public class DefaultResponseCommand extends RemotingCommand{

    private BaseResponse appResponse;

    private Throwable throwable;

    public DefaultResponseCommand() {
        super(CommandCode.Biz_Response);
    }

    public DefaultResponseCommand(int messageId) {
        super(CommandCode.Biz_Response, messageId);
    }

    @Override
    protected void serializeHeaders() {

    }

    @Override
    protected void serializeContent() {
        boolean hasContent = getAppResponse() != null;
        if (!hasContent)
            return;

        byte serialization = this.getSerialization();
        Serializer serializer = SerializerFactory.getSerializer(serialization);
        byte[] contentBytes = serializer.serialize(getAppResponse());
        this.setContentBytes(contentBytes);
    }

    @Override
    protected void deserializeHeaders() {

    }

    @Override
    protected void deserializeContent() {
        boolean hasContent = getContentBytes() != null && getMsgBytesLen() > 0;
        if (!hasContent)
            return;

        byte serialization = this.getSerialization();
        Serializer serializer = SerializerFactory.getSerializer(serialization);
        BaseResponse result = serializer.deserialize(this.getContentBytes(), BaseResponse.class);
        this.setAppResponse(result);
    }

    public BaseResponse getAppResponse() {
        return appResponse;
    }

    public void setAppResponse(BaseResponse appResponse) {
        this.appResponse = appResponse;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
