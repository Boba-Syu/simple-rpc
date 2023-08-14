package cn.bobasyu.core.common;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static cn.bobasyu.core.common.RpcConstants.MAGIC_NUMBER;

/**
 * RPC解码器，需要考虑战粘包拆包问题，以及设置请求数据包体积最大值
 */
public class RpcDecoder extends ByteToMessageDecoder {
    /**
     * 协议开头部分的标准长度
     */
    public final int BASE_LENGTH = 2 + 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        if (byteBuf.readableBytes() >= BASE_LENGTH) {
            // 防止收到一些体积过大的包
            if (byteBuf.readableBytes() > 1000) {
                byteBuf.skipBytes(byteBuf.readableBytes());
            }
            int beginReader = byteBuf.readerIndex();
            byteBuf.markReaderIndex();
            // RpcProtocol的魔数
            if (byteBuf.readShort() != MAGIC_NUMBER) {
                // 不是魔数开头，说明是非法的客户端发来的数据包
                ctx.close();
                return;
            }

            // RpcProtocol的contentLength字段
            int length = byteBuf.readInt();
            if (byteBuf.readableBytes() < length) {
                // 长度不匹配，说明数据包不完整
                byteBuf.readerIndex(beginReader);
                return;
            }
            // 解析content字段
            byte[] data = new byte[length];
            byteBuf.readBytes(data);
            RpcProtocol protocol = new RpcProtocol(data);
            out.add(protocol);
        }
    }
}
