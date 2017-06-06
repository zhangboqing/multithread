package com.zbq.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

/**
 * Created by zhangboqing on 2017/6/5.
 *
 * nio 客户端
 * 核心的元素也是Selector、Channel和SelectionKey
 */
public class NIOEchoClient {

    private Selector selector;

    /**
     * 进行初始化Selector和Channel
     */
    public void init(String ip, int port) throws IOException {
        //创建一个SocketChannel实例，并设置为非阻塞模式
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        //创建Selector
        this.selector = SelectorProvider.provider().openSelector();
        //将SocketChannel绑定到Socket上
        //由于当前Channel是非阻塞的，因此，connect()方法返回时，连接并不一定建立成功，在后续使用这个连接时，还需要使用finishConnect()再次确认
        channel.connect(new InetSocketAddress(ip, port));
        //将这个Channel和Selector进行绑定，并注册了感兴趣的事件作为连接（OP_CONNECT）
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    /**
     * 主要执行程序
     */
    public void working() throws IOException {
        while (true) {
            if (!selector.isOpen()) break;
            //通过Selector得到已经准备好的事件。如果当前没有任何事件准备就绪，这里就会阻塞
            selector.select();
            Iterator<SelectionKey> ite = this.selector.selectedKeys().iterator();
            //主要处理两个事件，首先是表示连接就绪的Connct事件（由connect()函数处理）以及表示通道可读的Read事件（由read()函数处理）
            while (ite.hasNext()) {
                SelectionKey key = ite.next();
                ite.remove();
                // 连接事件发生
                if (key.isConnectable()) {
                    connect(key);

                } else if (key.isReadable()) {
                    read(key);

                }
            }
        }
    }

    public void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        // 首先判断是否连接已经建立，如果没有，则调用finishConnect()完成连接
        if (channel.isConnectionPending()) {
            channel.finishConnect();

        }
        channel.configureBlocking(false);
        //建立连接后，向Channel写入数据，并同时注册读事件为感兴趣的事件
        channel.write(ByteBuffer.wrap(new String("hello server!\r\n").getBytes()));
        channel.register(this.selector, SelectionKey.OP_READ);
    }


    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        // 创建读取的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(100);
        channel.read(buffer);
        byte[] data = buffer.array();
        String msg = new String(data).trim();

        System.out.println("客户端收到信息：" + msg);
        channel.close();
        key.selector().close();

    }

    /**
     * 启动客户端
     * @param args
     */
    public static void main(String[] args) throws IOException {
        NIOEchoClient nioEchoClient = new NIOEchoClient();
        nioEchoClient.init("localhost",8000);
        nioEchoClient.working();
    }
}
