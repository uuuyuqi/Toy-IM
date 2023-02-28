package me.yq.remoting.transport.serializer;

/**
 * 序列化器接口
 * @author yq
 * @version v1.0 2023-02-13 0:18
 */
public interface Serializer {

    byte[] serialize(Object object);

    <T> T deserialize(byte[] bytes,Class<T> clazz);

    /**
     * 获取序列化器的 code
     * @return
     */
    byte code();
}
