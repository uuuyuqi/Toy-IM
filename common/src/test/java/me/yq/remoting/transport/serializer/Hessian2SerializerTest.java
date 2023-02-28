package me.yq.remoting.transport.serializer;

import me.yq.remoting.transport.domain.Friend;
import me.yq.remoting.transport.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Hessian2SerializerTest {
    private final Hessian2Serializer hessian2Serializer = new Hessian2Serializer();

    @Test
    @DisplayName("测试 hessian2 序列化和反序列化")
    void serializeAndDeserialize() {
        Friend friend = new Friend("zhangsan", 12);
        User user = new User(222, friend);
        byte[] serialize = hessian2Serializer.serialize(user);


        User result = hessian2Serializer.deserialize(serialize, User.class);
        Friend friendFromUser = (Friend) result.getFriend();
        assertEquals(friend.toString(),friendFromUser.toString());
    }
}