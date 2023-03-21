package me.yq.remoting.transport.command;

/**
 * 消息类型枚举 <br/>
 * <p>目前消息只支持两种类型：1.普通业务消息 2.心跳消息
 *
 * @author yq
 * @version v1.0 2023-02-12 23:58
 */
public enum CommandCode {

    Heartbeat((byte) 0),
    HeartbeatAck((byte) 1),


    Biz_Request((byte) 11),
    Biz_Response((byte) 12);

    private final byte cmd;

    CommandCode(byte cmd) {
        this.cmd = cmd;
    }

    public byte code() {
        return cmd;
    }

    public static CommandCode lookup(byte cmd){
        for (CommandCode value : CommandCode.values()) {
            if (value.cmd == cmd)
                return value;
        }
        return null;
    }

    public static boolean isLegalCmd(byte cmd){
        return lookup(cmd) != null;
    }
}
