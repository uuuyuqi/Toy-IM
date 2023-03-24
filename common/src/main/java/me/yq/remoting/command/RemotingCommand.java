package me.yq.remoting.command;

import me.yq.common.exception.SystemException;
import me.yq.remoting.constant.DefaultConfig;
import me.yq.remoting.support.RequestFutureMap;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通信对象，该对象是 IO 线程 和 biz 线程之间的信息交互对象。
 * IO 线程拿到该对象会转为 ByteBuf 并进行网络发送。<br/>
 * <p>
 * 通信对象分为两部分组成：
 * 1.基本信息字段  ——  存储通信对象的基本信息
 * 2.传输记录字段  ——  编解码后
 * </p>
 * @author yq
 * @version v1.0 2023-02-12 11:13
 */
public abstract class RemotingCommand implements Serializable {
    //======================================================================
    // ctrl flags
    //======================================================================
    /**
     * 传输协议版本，默认 YQ 1.0
     */
    private byte version = DefaultConfig.DEFAULT_PROTOCOL_VERSION;

    /**
     * 传输类型，目前支持：普通业务消息发送(登录、登出、消息发送)、心跳
     */
    private CommandCode cmd;

    /**
     * 序列化方式
     */
    private byte serialization = DefaultConfig.DEFAULT_SERIALIZATION;

    static private final AtomicInteger MESSAGE_ID_GENERATOR = new AtomicInteger(0);

    /**
     * 全局唯一的 msg_id, 用来做请求和响应的映射, 在消息记录上下文中会被缓存起来，参考：{@link RequestFutureMap}
     */
    private int messageId;

    //======================================================================
    // biz data
    //======================================================================

    /**
     * 消息头
     */
    private byte[] headerBytes;

    /**
     * 消息体
     */
    private byte[] contentBytes;


    public RemotingCommand(CommandCode cmd) {
        this(cmd, MESSAGE_ID_GENERATOR.getAndIncrement());
    }

    public RemotingCommand(CommandCode cmd,int messageId) {
        this.cmd = cmd;
        this.messageId = messageId;
    }




    //=======================================================================
    // setter getter
    //=======================================================================

    public int computeTotalSize() {
        return getHeaderBytesLen() + getMsgBytesLen();
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte[] getHeaderBytes() {
        return headerBytes;
    }

    public void setHeaderBytes(byte[] headerBytes) {
        this.headerBytes = headerBytes;
    }

    public int getHeaderBytesLen() {
        return headerBytes == null ? 0 : headerBytes.length;
    }


    public byte[] getContentBytes() {
        return contentBytes;
    }

    public void setContentBytes(byte[] contentBytes) {
        this.contentBytes = Objects.requireNonNull(contentBytes);
    }

    public int getMsgBytesLen() {
        return contentBytes == null ? 0 : contentBytes.length;
    }

    public CommandCode getCmd() {
        return cmd;
    }

    public void setCmd(CommandCode cmd) {
        this.cmd = cmd;
    }

    public byte getVersion() {
        return version;
    }

    public byte getSerialization() {
        return serialization;
    }

    public void setSerialization(byte serialization) {
        this.serialization = serialization;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }


    // ========= 序列化 =========
    public void serialize() {
        try {
            // 1.serialize headers
            serializeHeaders();
            // 2.serialize content (msg)
            serializeContent();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new SystemException("序列化时出现异常! 原因: " + t.getMessage(), t);
        }
    }

    /**
     * 将业务对象头序列化为 字节数组，放入 command 通信对象中，子类应该可选地实现
     */
    abstract protected void serializeHeaders();

    /**
     * 将业务对象序列化为 字节数组，放入 command 通信对象中，这里进行了一套通用的序列化流程
     */
    abstract protected void serializeContent();


    // ========= 反序列化 =========
    /**
     * 将 RemotingCommand 通信对象，将业务对象反序列化出来
     */
    public void deserialize(){
        try {
            // 1.deserialize headers
            deserializeHeaders();
            // 2.deserialize content (msg)
            deserializeContent();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new SystemException("反序列化时出现异常! 原因: " + t.getMessage(), t);
        }
    }

    /**
     * 反序列化头内容，将二进制内容头反序列化出来
     */
    abstract protected void deserializeHeaders();

    /**
     * 反序列化内容，将二进制内容反序列化出来
     */
    abstract protected void deserializeContent();

}
