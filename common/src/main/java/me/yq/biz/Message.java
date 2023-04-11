package me.yq.biz;

import me.yq.biz.domain.User;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息实体<br/>
 * 消息本身不分请求和响应，服务端也只会负责消息的转发
 * @author yq
 * @version v1.0 2023-02-14 5:29 PM
 */
public class Message  {

    private final int messageId = MSG_ID_GENERATOR.incrementAndGet();
    private static final AtomicInteger MSG_ID_GENERATOR = new AtomicInteger(0);

    private final User fromUser;

    private final User toUser;

    private final String msg;

    private final long sendTimestamp;


    public Message(User fromUser, User toUser, String msg) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.msg = msg;
        this.sendTimestamp = System.currentTimeMillis();
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public String getMsg() {
        return msg;
    }

    public long getSendTimestamp() {
        return sendTimestamp;
    }

    public int getMessageId() {
        return messageId;
    }
}
