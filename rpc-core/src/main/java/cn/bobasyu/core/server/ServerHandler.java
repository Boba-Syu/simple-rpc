package cn.bobasyu.core.server;


import cn.bobasyu.core.common.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


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
        ServerChannelReadData serverChannelReadData = new ServerChannelReadData();
        serverChannelReadData.setRpcProtocol((RpcProtocol) msg);
        serverChannelReadData.setChannelHandlerContext(ctx);
        // 放入channel分发器
        SERVER_CHANNEL_DISPATCHER.add(serverChannelReadData);
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
