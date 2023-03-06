package me.yq;

import lombok.extern.slf4j.Slf4j;
import me.yq.support.ChatClient;
import me.yq.support.ClientBootstrap;

/**
 * @author yq
 * @version v1.0 2023-03-01 10:39
 */
@Slf4j
public class TestMain {
    public static void main(String[] args) {
        ChatClient chatClient = new ClientBootstrap().buildClient();
        chatClient.start();

        log.info("正在登录......");
        chatClient.signIn(909900, "123456");

        chatClient.sendMsg(157146,"你在干嘛呢？");
    }
}
