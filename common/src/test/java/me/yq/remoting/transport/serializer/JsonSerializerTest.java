package me.yq.remoting.transport.serializer;

import me.yq.test.common.domain.Friend;
import me.yq.test.common.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JsonSerializer 测试类，主要覆盖的功能：
 * 1. json 序列化 和 反序列化
 */
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
    @DisplayName("测试 json 序列化和反序列引用类型")
    void cantAcceptObjField() {
        Friend friendInUser = new Friend("zhangsan", 12);
        User user = new User(222, friendInUser);
        byte[] serialize = jsonSerializer.serialize(user);

        User userResult = jsonSerializer.deserialize(serialize, User.class);
        Friend friendResult = userResult.getFriend();
        assertEquals(friendResult.toString(),friendInUser.toString(),"序列化和反序列化的对象应该一致");
    }
}