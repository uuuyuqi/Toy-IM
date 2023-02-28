package me.yq;

import me.yq.remoting.RemotingServer;
import me.yq.support.ServerBootstrap;

/**
 * 服务端
 * @author yq
 * @version v1.0 2023-02-14 5:33 PM
 */
public class ChatServer {

    public void start() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        RemotingServer server = new RemotingServer(serverBootstrap);
        server.start();
    }
}
