package me.yq.remoting.transport.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂，可以根据 序列化器代号(code) 获取序列化器
 *
 * @author yq
 * @version v1.0 2023-02-13 9:25 AM
 */
public class SerializerFactory {
    private static final Map<Byte, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>(10);

    static {
        SERIALIZER_MAP.put(Hessian2Serializer.CODE,new Hessian2Serializer());
        SERIALIZER_MAP.put(JsonSerializer.CODE,new JsonSerializer());
    }

    public static void registerSerializer(byte code, Serializer serializer) {
        if (SERIALIZER_MAP.containsKey(code))
            throw new RuntimeException("序列化器存在重复!");
        SERIALIZER_MAP.put(code, serializer);
    }

    public static Serializer getSerializer(byte code) {
        if (!SERIALIZER_MAP.containsKey(code))
            throw new RuntimeException("序列化器不存在!");
        return SERIALIZER_MAP.get(code);
    }

}
