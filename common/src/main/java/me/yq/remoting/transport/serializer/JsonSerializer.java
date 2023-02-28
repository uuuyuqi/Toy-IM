package me.yq.remoting.transport.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Json 序列化反序列化器
 * @author yq
 * @version v1.0 2023-02-13 0:22
 */
public class JsonSerializer implements Serializer {

    public static final byte CODE = (byte) 2;

    @Override
    public byte[] serialize(Object object) {
        String json = JSON.toJSONString(object);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            String json = new String(bytes,StandardCharsets.UTF_8);
            // todo: bytes 转 object
            return JSONObject.parseObject(json,clazz);
        } catch (Exception e) {
            throw new RuntimeException("序列化时出现异常: " + e.getMessage(),e);
        }

    }

    @Override
    public byte code() {
        return CODE;
    }
}
