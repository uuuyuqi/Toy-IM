package me.yq.support;

import me.yq.remoting.RemotingServer;

/**
 * 服务端
 * @author yq
 * @version v1.0 2023-02-14 5:33 PM
 */
public class ChatServer {

    private final RemotingServer server;

    public ChatServer(RemotingServer server) {
        this.server = server;
    }

    public void start() {
        server.start();
    }
}
