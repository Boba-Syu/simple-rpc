package cn.bobasyu.core.server;


import cn.bobasyu.core.common.RpcInvocation;
import cn.bobasyu.core.common.RpcProtocol;
import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;

import static cn.bobasyu.core.common.cache.CommonServerCache.*;

/**
 * 服务器接收到数据后的处理类
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 根据收到的信息解析出RpcInvocation，即远程调用方法信息，并通过反射的方式调用响应方法，拿到结果并返回
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 服务端接收数据统一以RPCProtocol协议的格式接收
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);
        // 责任链调用
        SERVER_FILTER_CHAN.doFilter(rpcInvocation);
        // PROVIDER_CLASS_MAP为预先在启动时存储的Bean的集合
        Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetMethod());
        Method[] methods = aimObject.getClass().getDeclaredMethods();
        Object result = null;
        for (Method method : methods) {
            // 通过反射找到目标函数，然后执行目标方法并返回对应值
            if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                if (method.getReturnType().equals(Void.TYPE)) {
                    method.invoke(aimObject, rpcInvocation.getArgs());
                } else {
                    result = method.invoke(aimObject, rpcInvocation.getArgs());
                }
                break;
            }
        }
        rpcInvocation.setResponse(result);
        RpcProtocol respPpcProtocol = new RpcProtocol(JSON.toJSONString(rpcInvocation).getBytes());
        ctx.writeAndFlush(respPpcProtocol);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
