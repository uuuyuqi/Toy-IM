package me.yq.remoting.transport.serializer;

import me.yq.remoting.transport.domain.Friend;
import me.yq.remoting.transport.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSerializerTest {
    private final JsonSerializer jsonSerializer = new JsonSerializer();


    @Test
    @DisplayName("测试 json 序列化和反序列化")
    void serializeAndDeserialized() {

        Map<String,String> bizMap = new HashMap<String,String>(){
            {
                put("name","zs");
                put("age","12");
            }
        };
        byte[] serialize = jsonSerializer.serialize(bizMap);

        @SuppressWarnings("rawtypes")
        Map deserialize = jsonSerializer.deserialize(serialize, Map.class);

        assertEquals("{name=zs, age=12}",deserialize.toString());

    }

    @Test
    @DisplayName("测试 json 序列化和反序列化 【不能】 接收引用类型")
    void cantAcceptObjField() {
        User user = new User(222, new Friend("zhangsan", 12));
        byte[] serialize = jsonSerializer.serialize(user);

        User result = jsonSerializer.deserialize(serialize, User.class);

        ClassCastException ex = Assertions.assertThrows(ClassCastException.class, () -> {
            Friend friend = (Friend) result.getFriend();
        });

        assertTrue(ex.getMessage().contains("cannot be cast to"));
    }
}