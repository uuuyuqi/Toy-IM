package me.yq.remoting.transport.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * kryo 序列化器
 * @author yq
 * @version v1.0 2023-03-12 19:10
 */
public class KryoSerializer implements Serializer{

    public static final byte CODE = (byte) 3;

    // 无需清理，救火线程的 ThreadLocal 的 value 会被 gc 掉的
    private final ThreadLocal<Kryo> localKryo = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);// 支持循环引用（不过默认值也就是true）
        kryo.setRegistrationRequired(false);// 关闭注册行为
        return kryo;
    });

    @SneakyThrows
    @Override
    public byte[] serialize(Object object) {
        Kryo kryo = localKryo.get();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output,object);
        output.close();
        return baos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Kryo kryo = localKryo.get();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Input input = new Input(bais);
        input.close();
        return (T) kryo.readClassAndObject(input);
    }

    @Override
    public byte code() {
        return CODE;
    }


}