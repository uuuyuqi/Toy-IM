package me.yq.remoting.command;

/**
 * 心跳
 * @author yq
 * @version v1.0 2023-02-12 23:56
 */
public class HeartbeatCommand extends RemotingCommand {

    public HeartbeatCommand() {
        super(CommandCode.Heartbeat);
    }

    public HeartbeatCommand(int requestId) {
        super(CommandCode.Heartbeat,requestId);
    }


    @Override
    protected void serializeHeaders() {
    }

    @Override
    protected void deserializeHeaders() {
    }

    @Override
    protected void serializeContent() {
    }

    @Override
    protected void deserializeContent() {
    }
}
