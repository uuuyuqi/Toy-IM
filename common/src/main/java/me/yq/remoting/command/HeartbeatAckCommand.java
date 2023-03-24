package me.yq.remoting.command;

/**
 * @author yq
 * @version v1.0 2023-03-07 20:35
 */
public class HeartbeatAckCommand extends RemotingCommand {

    public HeartbeatAckCommand() {
        super(CommandCode.HeartbeatAck);
    }

    public HeartbeatAckCommand(int requestId) {
        super(CommandCode.HeartbeatAck,requestId);
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
