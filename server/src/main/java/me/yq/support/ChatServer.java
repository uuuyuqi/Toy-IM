package me.yq.support;

import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.RemotingServer;

/**
 * 服务端
 * @author yq
 * @version v1.0 2023-02-14 5:33 PM
 */
@Slf4j
public class ChatServer {

    private final RemotingServer server;

    public ChatServer(RemotingServer server) {
        this.server = server;
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.shutdown(5000);
        log.info("服务端已关闭!");
    }
}
