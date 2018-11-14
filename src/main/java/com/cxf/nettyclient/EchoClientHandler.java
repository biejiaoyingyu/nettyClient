package com.cxf.nettyclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * 为什么我们在客户端使用的是 SimpleChannelInboundHandler，而不是在 EchoServerHandler 中所使用的
 * ChannelInboundHandlerAdapter 呢？这和两个因素的相互作用有关：业务逻辑如何处理消息以及 Netty 如何管理资源。
 *
 * A.在客户端，当channelRead0()方法完成时，你已经有了传入消息，并且已经处理完它了。
 *   当该方法返回时，SimpleChannelInboundHandler 负责释放指向保存该消息的ByteBuf
 *   的内存引用。
 * B.在EchoServerHandler中，你仍然需要将传入消息回送给发送者，而 write()操作是异步
 *   的，直到channelRead()方法返回后可能仍然没有完成。为此，EchoServerHandler扩展了
 *   ChannelInboundHandlerAdapter，其在这个时间点上不会释放消息。
 * c.消息在 EchoServerHandler 的 channelReadComplete()方法中，当writeAndFlush()
 *   方法被调用时被释放
 */
@ChannelHandler.Sharable
public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    //当被通知 Channel是活跃的时候，发送一条消息
    //重写channelActive()方法，其将在一个连接建立时被调用。这确保了数据将会被尽可能快
    //地写入服务器，其在这个场景下是一个编码了字符串"Netty rocks!"的字节缓冲区
    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!", CharsetUtil.UTF_8));
    }

    //记录已接收消息的转储
    //重写了 channelRead0()方法。每当接收数据时，都会调用这个方法。需要注意的是，由服务器
    //发送的消息可能会被分块接收。也就是说，如果服务器发送了5字节，那么不能保证这 5 字节会
    //被一次性接收。即使是对于这么少量的数据，channelRead0()方法也可能会被调用两次，第一次
    //使用一个持有 3 字节的 ByteBuf（Netty 的字节容器），第二次使用一个持有 2 字节的ByteBuf。
    //作为一个面向流的协议，TCP 保证了字节数组将会按照服务器发送它们的顺序被接收
    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println("Client received: " + in.toString(CharsetUtil.UTF_8));
    }

    //在发生异常时，记录错误并关闭Channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
