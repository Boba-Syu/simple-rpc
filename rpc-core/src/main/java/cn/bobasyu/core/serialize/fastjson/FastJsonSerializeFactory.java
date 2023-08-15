package cn.bobasyu.core.serialize.fastjson;

import cn.bobasyu.core.serialize.SerializeFactory;
import com.alibaba.fastjson.JSON;

public class FastJsonSerializeFactory implements SerializeFactory {

    @Override
    public <T> byte[] serialize(T t) {
        String jsonStr = JSON.toJSONString(t);
        return jsonStr.getBytes();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return JSON.parseObject(new String(data),clazz);
    }


}