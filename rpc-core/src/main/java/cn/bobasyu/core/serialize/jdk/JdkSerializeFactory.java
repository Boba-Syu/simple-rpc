package cn.bobasyu.core.serialize.jdk;

import cn.bobasyu.core.serialize.SerializeFactory;

import java.io.*;

public class JdkSerializeFactory implements SerializeFactory {
    @Override
    public <T> byte[] serialize(T t) {
        byte[] data = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(os)) {
            output.writeObject(t);
            output.flush();
            data = os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {

        try (ByteArrayInputStream is = new ByteArrayInputStream(data);
             ObjectInputStream input = new ObjectInputStream(is)) {
            Object result = input.readObject();
            return (T) result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
