package me.yq;

import me.yq.support.ChatServer;
import me.yq.support.ServerBootstrap;

/**
 * @author yq
 * @version v1.0 2023-02-23 16:11
 */
public class ServerStarter {
    public static void main(String[] args) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        ChatServer chatServer = bootstrap.buildServer();
        chatServer.start();
    }
}
