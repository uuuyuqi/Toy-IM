package me.yq.remoting.transport.serializer;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.JavaSerializer;
import com.caucho.hessian.io.SerializerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hessian2 序列化反序列化器
 * @author yq
 * @version v1.0 2023-02-24 09:18
 */
public class Hessian2Serializer implements Serializer{

    public static final byte CODE = (byte) 1;

    @Override
    public byte[] serialize(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(baos);
        try {
            // GenericSerializerFactory 不强制要求实现 jdk 的 序列化able 接口
            hessian2Output.setSerializerFactory(new MySerializerFactory());
            hessian2Output.writeObject(object);
        } catch (IOException e) {
            throw new RuntimeException("hessian2 序列化失败！异常信息： " + e.getMessage());
        }finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                hessian2Output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return baos.toByteArray();
    }


    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        T result;
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Hessian2Input hessian2Input = new Hessian2Input(bais);
        try {
            hessian2Input.setSerializerFactory(new MySerializerFactory());
            result = (T) hessian2Input.readObject(clazz);
        } catch (IOException e) {
            throw new RuntimeException("hessian2 反序列化失败！异常信息： " + e.getMessage());
        }finally {
            try {
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                hessian2Input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public byte code() {
        return CODE;
    }

    /**
     * 取消 被序列化对象 实现 Serializable 的约束。参考：{@link SerializerFactory#getDefaultSerializer(Class)}
     */
    private static class MySerializerFactory extends SerializerFactory {
        @Override
        protected com.caucho.hessian.io.Serializer getDefaultSerializer(Class cl) {
            if (_defaultSerializer != null)
                return _defaultSerializer;
            return JavaSerializer.create(cl);
        }
    }
}
