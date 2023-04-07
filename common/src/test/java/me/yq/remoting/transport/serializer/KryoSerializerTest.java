package me.yq.remoting.transport.serializer;

import me.yq.test.common.domain.Friend;
import me.yq.test.common.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * KryoSerializer 测试类，主要覆盖的功能：
 * 1. kryo 序列化 和 反序列化
 * 2. kryo 序列化器的线程安全
 * @author yq
 * @version v1.0 2023-04-07 11:37
 */
public class KryoSerializerTest {
    private final KryoSerializer kryoSerializer = new KryoSerializer();

    @Test
    @DisplayName("测试 kryo 序列化和反序列化")
    void serializeAndDeserialize() {
        Friend friend = new Friend("zhangsan", 12);
        User user = new User(222, friend);
        byte[] serialize = kryoSerializer.serialize(user);


        User result = kryoSerializer.deserialize(serialize, User.class);
        Friend friendFromUser = result.getFriend();
        assertEquals(friend.toString(),friendFromUser.toString(),"序列化和反序列化的对象应该一致");
    }


    @Test
    @DisplayName("测试 kryo 序列化器的线程安全")
    void serializeAndDeserialize_thread_safe() {

        CyclicBarrier barrier = new CyclicBarrier(100);
        Runnable task = () -> {
            // 保证一起跑
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {
            }

            Friend friend = new Friend("zhangsan", 12);
            User user = new User(222, friend);
            byte[] serialize = kryoSerializer.serialize(user);


            User result = kryoSerializer.deserialize(serialize, User.class);
            Friend friendFromUser = result.getFriend();
            assertEquals(friend.toString(),friendFromUser.toString(),"序列化和反序列化的对象应该一致");
        };

        // 拉起 100 个线程一起跑
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(task);
        }
    }
}
