package me.yq;

import me.yq.support.ChatClient;
import me.yq.support.ClientBootstrap;

import java.util.concurrent.TimeUnit;

/**
 * @author yq
 * @version v1.0 2023-02-23 16:11
 */
public class ClientStarter {

    public static void main(String[] args) {
        ChatClient chatClient = new ClientBootstrap().buildClient();
        chatClient.start();
        
        chatClient.signIn(157146, "abcde");

        System.out.println("now！ biz dealing！");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored) {
        }

        chatClient.signOut(157146);

    }
}
