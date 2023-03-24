package me.yq.remoting.command;

import me.yq.common.BaseRequest;
import me.yq.remoting.transport.serializer.Serializer;
import me.yq.remoting.transport.serializer.SerializerFactory;

/**
 * 基础通信请求对象
 * @author yq
 * @version v1.0 2023-02-16 1:55 PM
 */
public class DefaultRequestCommand extends RemotingCommand{

    /**
     * 业务传输对象
     */
    private BaseRequest appRequest;


    public DefaultRequestCommand() {
        super(CommandCode.Biz_Request);
    }

    public DefaultRequestCommand(int requestId) {
        super(CommandCode.Biz_Request,requestId);
    }

    protected void serializeHeaders(){

    }


    protected void serializeContent() {
        boolean hasContent = getAppRequest() != null;
        if (!hasContent)
            return;

        byte serialization = this.getSerialization();
        Serializer serializer = SerializerFactory.getSerializer(serialization);
        byte[] contentBytes = serializer.serialize(getAppRequest());
        this.setContentBytes(contentBytes);
    }


    protected void deserializeHeaders() {
    }


    protected void deserializeContent() {
        boolean hasContent = getContentBytes() != null && getMsgBytesLen() > 0;
        if (!hasContent)
            return;

        byte serialization = this.getSerialization();
        Serializer serializer = SerializerFactory.getSerializer(serialization);
        BaseRequest result = serializer.deserialize(this.getContentBytes(), BaseRequest.class);
        this.setAppRequest(result);
    }


    public BaseRequest getAppRequest() {
        return appRequest;
    }

    public void setAppRequest(BaseRequest appRequest) {
        this.appRequest = appRequest;
    }
}
