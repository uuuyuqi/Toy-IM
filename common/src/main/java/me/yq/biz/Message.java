package me.yq.biz;

import me.yq.biz.domain.User;

/**
 * 消息实体<br/>
 * 消息本身不分请求和响应，服务端也只会负责消息的转发
 * @author yq
 * @version v1.0 2023-02-14 5:29 PM
 */
public class Message  {
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

}
