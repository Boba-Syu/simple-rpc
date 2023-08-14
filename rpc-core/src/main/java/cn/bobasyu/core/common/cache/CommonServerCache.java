package cn.bobasyu.core.common.cache;

import cn.bobasyu.core.common.RpcInvocation;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class CommonServerCache {
    public static BlockingQueue<RpcInvocation> SEND_QUEUE = new ArrayBlockingQueue(100);

    public static Map<String, Object> RESP_MAP = new ConcurrentHashMap<>();

    public static Map<String, Object> PROVIDER_CLASS_MAP = new ConcurrentHashMap<>();
}
