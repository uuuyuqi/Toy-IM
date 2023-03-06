package me.yq.remoting.transport.serializer;

import me.yq.remoting.test.common.domain.Friend;
import me.yq.remoting.test.common.domain.MyLinkedHashMap;
import me.yq.remoting.test.common.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    @DisplayName("测试集合子类的 hessian2 序列化和反序列化")
    void serializeAndDeserialize_special_class() {
        MyLinkedHashMap map = new MyLinkedHashMap();
        map.put("k1","v1");
        map.put("k2","v2");
        map.setVar1("a");
        map.setVar2("b");
        map.setVar3("c");
        byte[] serialize = hessian2Serializer.serialize(map);


        MyLinkedHashMap result = hessian2Serializer.deserialize(serialize, MyLinkedHashMap.class);
        assertNull(result.getVar1());
        assertNull(result.getVar2());
        assertNull(result.getVar3());
    }
}