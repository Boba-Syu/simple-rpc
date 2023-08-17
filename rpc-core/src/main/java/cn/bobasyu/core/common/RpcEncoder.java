package cn.bobasyu.core.common;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import static cn.bobasyu.core.common.RpcConstants.DEFAULT_DECODE_CHAR;

/**
 * RPC请求编码器
 */
public class RpcEncoder extends MessageToByteEncoder<RpcProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getMagicNumber());
        out.writeInt(msg.getContentLength());
        out.writeBytes(msg.getContent());
        // 数据包过大导致netty拆分成多个包时出现的异常
        out.writeBytes(DEFAULT_DECODE_CHAR.getBytes());
    }
}
