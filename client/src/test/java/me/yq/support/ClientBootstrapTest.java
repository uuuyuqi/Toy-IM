package me.yq.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBootstrapTest {

    @Test
    @DisplayName("测试客户端不应该重复创建")
    void buildClient() {
        ClientBootstrap bootstrap = new ClientBootstrap();
        ChatClient chatClient = bootstrap.buildClient();

        RuntimeException ex = assertThrows(RuntimeException.class, bootstrap::buildClient);
        assertTrue(ex.getMessage().contains("请勿重复创建"));
    }
}