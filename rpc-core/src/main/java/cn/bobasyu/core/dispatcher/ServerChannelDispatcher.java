package cn.bobasyu.core.dispatcher;

import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.common.RpcProtocol;
import cn.bobasyu.core.exception.RpcException;
import cn.bobasyu.core.server.ServerChannelReadData;

import java.lang.reflect.Method;
import java.util.concurrent.*;

import static cn.bobasyu.core.common.cache.CommonServerCache.*;

public class ServerChannelDispatcher {

    private BlockingQueue<ServerChannelReadData> RPC_DATA_QUEUE;

    private ExecutorService executorService;

    public void init(int queueSize, int bizThreadNums) {
        RPC_DATA_QUEUE = new ArrayBlockingQueue<>(queueSize);
        executorService = new ThreadPoolExecutor(bizThreadNums, bizThreadNums, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(512));
    }

    public void add(ServerChannelReadData serverChannelReadData) {
        RPC_DATA_QUEUE.add(serverChannelReadData);
    }

    public void startDataConsume() {
        Thread thread = new Thread(new ServerJobCoreHandler());
        thread.start();
    }

    private class ServerJobCoreHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    ServerChannelReadData serverChannelReadData = RPC_DATA_QUEUE.take();
                    executorService.execute(() -> {
                        RpcProtocol rpcProtocol = serverChannelReadData.getRpcProtocol();
                        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);
                        System.out.println("rpcInvocation:" + rpcInvocation.getTargetServiceName());
                        System.out.println("serialize:" + SERVER_SERIALIZE_FACTORY);
                        // 执行过滤请求
                        try {
                            SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInvocation);
                        } catch (Exception e) {
                            if (e instanceof RpcException) {
                                RpcException rpcException = (RpcException) e;
                                RpcInvocation repParam = rpcException.getRpcInvocation();
                                rpcInvocation.setE(e);
                                byte[] body = SERVER_SERIALIZE_FACTORY.serialize(repParam);
                                RpcProtocol respRpcProtocol = new RpcProtocol(body);
                                serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
                                return;
                            }
                        }
                        // 从服务提供者中获取目标服务
                        Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
                        // 获取目标服务的全部方法
                        Method[] methods = aimObject.getClass().getDeclaredMethods();
                        Object result = null;
                        // 寻找得到对应的并执行
                        for (Method method : methods) {
                            try {
                                if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                                    if (method.getReturnType().equals(Void.TYPE)) {
                                        method.invoke(aimObject, rpcInvocation.getArgs());
                                    } else {
                                        result = method.invoke(aimObject, rpcInvocation.getArgs());
                                    }
                                    break;
                                }
                            } catch (Exception e) {
                                rpcInvocation.setE(e);
                            }
                            // 设置response
                            rpcInvocation.setResponse(result);
                            // 后置过滤器
                            SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInvocation);
                            // 再次封装为RpcProtocol返回给客户端
                            RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
                            serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
