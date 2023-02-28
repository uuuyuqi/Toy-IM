package me.yq.remoting.codec.protocol;


import me.yq.remoting.transport.command.CommandCode;
import me.yq.remoting.transport.command.DefaultRequestCommand;
import me.yq.remoting.transport.command.DefaultResponseCommand;
import me.yq.remoting.transport.command.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * 协议编解码器。通讯层采用的是 YQ 协议(应用层通信协议)，大体按 TLV 格式来组织，如下图 (宽度单位: byte)
 * <pre>
 * 0          1           2            3            4
 * +----------+-----------+------------+------------+
 * |magic-bits|  version  |    cmd     |  serialize |
 * +----------+-----------+------------+------------+
 * |                  message-id                    |
 * +------------------------------------------------+
 * |                  total-content-size            |
 * +------------------------------------------------+
 * |                  header-len                    |
 * +------------------------------------------------+
 * |                                                |
 * |                  header ...                    |
 * |                                                |
 * +------------------------------------------------+
 * |                  content-len                   |
 * +---------------------+--------------------------+
 * |                                                |
 * |                  content...                    |
 * |                                                |
 * +------------------------------------------------+
 * <p>注：蚂蚁金服开源的 bolt 框架提供的 bolt 协议设计非常巧妙，
 * 直接将 3 个 len 合在一起，可以直接去掉 totalSize 字段</p>
 * </pre>
 *
 * @author yq
 * @version v1.0 2023-02-12 17:32
 */
@Slf4j
public class YQCommandCodec implements Codec {

    private static final byte MAGIC_CODE = 0b0101_0101;


    /**
     * 控制字段的最小长度，解包时必须满足该长度才认为允许本次解包，否则继续积累数据！<br/>
     */
    private final short lowestLen =
            1/*magic-code*/ + 1/*version*/ + 1/*cmd*/ + 1/*serialize*/
                    + 4/*message-id*/ + 4/*total-content-size*/
                    + 4/*header-len*/ + 4/*content-len*/;


    /**
     * 协议编码，可以把 Command 对象装入 netty 的 ByteBuf
     *
     * @param in  待序列化对象，应该是 Command 对象，该对象的所有字段被序列化成 bytes
     * @param out 装入序列化结果的 ByteBuf 对象
     */
    @Override
    public void encode(Serializable in, ByteBuf out) {
        if (in instanceof RemotingCommand) {
            RemotingCommand command = (RemotingCommand) in;

            //   0          1           2            3            4
            //   +----------+-----------+------------+------------+
            //   |magic-bits|  version  |    cmd     |  serialize |
            //   +----------+-----------+------------+------------+
            //   |                  message-id                    |
            //   +------------------------------------------------+
            //   |                  total-content-size            |
            //   +------------------------------------------------+
            //   |                  header-len                    |
            //   +------------------------------------------------+
            //   |                                                |
            //   |                  header ...                    |
            //   |                                                |
            //   +------------------------------------------------+
            //   |                  content-len                   |
            //   +---------------------+--------------------------+
            //   |                                                |
            //   |                  content...                    |
            //   |                                                |
            //   +------------------------------------------------+

            // control fields
            out.writeByte(MAGIC_CODE);
            out.writeByte(command.getVersion());
            out.writeByte(command.getCmd().code());
            out.writeByte(command.getSerialization());
            out.writeInt(command.getMessageId());
            out.writeInt(command.computeTotalSize());

            // header fields
            int headerBytesLen = command.getHeaderBytesLen();
            out.writeInt(headerBytesLen);
            if (headerBytesLen > 0)
                out.writeBytes(command.getHeaderBytes());

            // msg content fields
            int msgBytesLen = command.getMsgBytesLen();
            out.writeInt(msgBytesLen);
            if (msgBytesLen > 0)
                out.writeBytes(command.getContentBytes());

        } else {
            log.error("待编码的参数必须是 RemotingCommand 的子类，请检查参数!");
        }

    }


    /**
     * 协议解码，负责将 ByteBuf 解析成业务层可以识别的对象
     *
     * @param in  待解码的 ByteBuf 对象
     * @param out 解码结果，一般只会是 Command 对象
     */
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 暂存一下当前的 reader 指针，如果 decode 失败，复位 buf 的指针
        // 比如发生了半包(结合具备 Cumulation 能力的上层 Decoder)，就需要等上层继续收集足够的数据满足一个包再来 decode
        in.markReaderIndex();
        try {
            boolean canDecode = (in.readableBytes() > 1) && in.readByte() == MAGIC_CODE;
            if (!canDecode) {
                in.resetReaderIndex();
                return;
            }

            //==========================================
            // part1 read control fields
            //==========================================
            if (in.readableBytes() < this.lowestLen) { //确保 totalContentSize 能读出来
                in.resetReaderIndex();
                log.warn("尝试按 YQ 协议解包，但是内容长度没有达到协议的最小长度！");
                return;
            }

            byte version = in.readByte();
            byte cmd = in.readByte();
            if (!CommandCode.isLegalCmd(cmd))
                throw new RuntimeException("不支持的请求类型: " + cmd);
            byte serialization = in.readByte();
            int msgId = in.readInt();
            int totalContentSize = in.readInt();

            if (in.readableBytes() < totalContentSize) { //确保 header、class、object 能读出来
                in.resetReaderIndex();
                return;
            }

            //==========================================
            // part2 read content fields
            //==========================================
            byte[] header = null;
            int headerLen = in.readInt();
            if (headerLen > 0){
                header = new byte[headerLen];
                in.readBytes(header);
            }

            byte[] msg = null;
            int msgLen = in.readInt();
            if (msgLen > 0){
                msg = new byte[msgLen];
                in.readBytes(msg);
            }

            //==========================================
            // part3 assembly the real msg
            //==========================================
            if (cmd == CommandCode.Biz_Request.code()){
                RemotingCommand command = new DefaultRequestCommand();

                // put ctrl flags
                command.setVersion(version);
                command.setCmd(CommandCode.Biz_Request);
                command.setSerialization(serialization);
                command.setMessageId(msgId);

                // put data
                command.setHeaderBytes(header);
                command.setContentBytes(msg);
                out.add(command);
            }
            else if (cmd == CommandCode.Biz_Response.code()){
                RemotingCommand command = new DefaultResponseCommand();

                // put ctrl flags
                command.setVersion(version);
                command.setCmd(CommandCode.Biz_Request);
                command.setSerialization(serialization);
                command.setMessageId(msgId);

                // put data
                command.setHeaderBytes(header);
                command.setContentBytes(msg);
                out.add(command);
            }
            else if (cmd == CommandCode.Heartbeat.code()) {
               // todo
            }

        } catch (Throwable t) {
            throw new RuntimeException("解码过程中遭遇问题: " + t.getMessage(),t);
        }

    }


    /**
     * 判断是否可以进行 decode
     *
     * @param magicCode 魔法数字
     * @return 可以 decode 则返回 true
     */
    public boolean canDecode(byte magicCode) {
        return magicCode == MAGIC_CODE;
    }
}
