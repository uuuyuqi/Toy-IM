package me.yq.biz;

/**
 * 消息实体应答实体<br/>
 * 消息发送未必成功，如果失败，服务端应该告知客户端失败！
 * @author yq
 * @version v1.0 2023-02-14 5:29 PM
 */
public class MessageAck {
    private final int msgId;

    private final boolean ok;

    public MessageAck(int msgId, boolean ok) {
        this.msgId = msgId;
        this.ok = ok;
    }

    public int getMsgId() {
        return msgId;
    }

    public boolean isOk() {
        return ok;
    }
}
