package com.zbq.io.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhangboqing on 2017/6/5.
 *
 * nio 服务端
 *
 * 作用：Java的NIO可以将网络IO等待时间从业务处理线程中抽取出来。
 */
public class NioEchoServer {

    /**
     * 1.selector用于处理所有的网络连接
     * 2.线程池tp用于对每一个客户端进行相应的处理，每一个请求都会委托给线程池中的线程进行实际的处理
     * 3.time_stat它用于统计在某一个Socket上花费的时间，time_stat的key为Socket，value为时间戳（可以记录处理开始时间）。
     */
    private Selector selector;
    private ExecutorService tp = Executors.newCachedThreadPool();
    public static Map<Socket, Long> time_stat = new HashMap<Socket, Long>(10240);

    /**
     * 用于启动NIO Server
     */
    private void startServer() throws Exception {
        //通过工厂方法获得一个Selector对象的实例
        selector = SelectorProvider.provider().openSelector();
        //获得表示服务端的SocketChannel实例
        ServerSocketChannel ssc = ServerSocketChannel.open();
        //将这个SocketChannel设置为非阻塞模式。实际上，Channel也可以像传统的Socket那样按照阻塞的方式工作
        //但在这里，更倾向于让其工作在非阻塞模式，在这种模式下，我们才可以向Channel注册感兴趣的事件，并且在数据准备好时，得到必要的通知
        ssc.configureBlocking(false);
        //将Channel绑定在8000端口。
        InetSocketAddress isa = new InetSocketAddress(/*InetAddress.getLocalHost()*/"localhost", 8000);
        ssc.socket().bind(isa);
        //将ServerSocketChannel绑定到Selector上，并注册它感兴趣的事件为Accept
        //当Selector发现ServerSocketChannel有新的客户端连接时，就会通知ServerSocketChannel进行处理。
        //方法register()的返回值是一个SelectionKey，SelectionKey表示一对Selector和Channel的关系。
        //当Channel注册到Selector上时，就相当于确立了两者的服务关系，那么SelectionKey就是这个契约。
        //当Selector或者Channel被关闭时，它们对应的SelectionKey就会失效。
        SelectionKey acceptKey = ssc.register(selector, SelectionKey.OP_ACCEPT);
        //无穷循环，它的主要任务就是等待-分发网络消息
        for (; ; ) {
            //select()方法是一个阻塞方法。如果当前没有任何数据准备好，它就会等待。一旦有数据可读，它就会返回。它的返回值是已经准备就绪的SelectionKey的数量。
            selector.select();
            //获取那些准备好的SelectionKey
            Set readyKeys = selector.selectedKeys();
            Iterator i = readyKeys.iterator();
            long e = 0;
            while (i.hasNext()) {
                SelectionKey sk = (SelectionKey) i.next();
                //将这个元素移除！注意，这个非常重要，否则就会重复处理相同的SelectionKey
                i.remove();
                //判断当前SelectionKey所代表的Channel是否在Acceptable状态，如果是，就进行客户端的接收（执行doAccept()方法）
                if (sk.isAcceptable()) {
                    doAccept(sk);
                    //判断Channel是否已经可以读了，如果是就进行读取（doRead()方法）
                } else if (sk.isValid() && sk.isReadable()) {
                    if (!time_stat.containsKey(((SocketChannel) sk.channel()).socket()))
                        time_stat.put(((SocketChannel) sk.channel()).socket(), System.currentTimeMillis());
                    doRead(sk);
                    //判断通道是否准备好进行写。如果是就进行写入（doWrite()方法），同时在写入完成后，根据读取前的时间戳，输出处理这个Socket连接的耗时。
                } else if (sk.isValid() && sk.isWritable()) {

                    doWrite(sk);
                    e = System.currentTimeMillis();
                    long b = time_stat.remove(((SocketChannel) sk.channel()).socket());
                    System.out.println("spend:" + (e - b) + "ms");

                }
            }
        }
    }



    /**
     * 和Socket编程很类似，当有一个新的客户端连接接入时，就会有一个新的Channel产生代表这个连接。
     * 生成的clientChannel就表示和客户端通信的通道。
     */
    private void doAccept(SelectionKey sk) {
        ServerSocketChannel server = (ServerSocketChannel) sk.channel();
        SocketChannel clientChannel;
        try {
            clientChannel = server.accept();
            //将这个Channel配置为非阻塞模式，也就是要求系统在准备好IO后，再通知我们的线程来读取或者写入。
            clientChannel.configureBlocking(false);
            // Register this channel for reading.
            //将新生成的Channel注册到selector选择器上，并告诉Selector，我现在对读（OP_READ）操作感兴趣。
            // 这样，当Selector发现这个Channel已经准备好读时，就能给线程一个通知。
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            // Allocate an EchoClient instance and attach it to this selection key.
            //一个EchoClient实例代表一个客户端。
            // 我们将这个客户端实例作为附件，附加到表示这个连接的SelectionKey上。
            // 这样在整个连接的处理过程中，我们都可以共享这个EchoClient实例。
            EchoClient echoClient = new EchoClient();
            clientKey.attach(echoClient);
            InetAddress clientAddress = clientChannel.socket().getInetAddress();
            System.out.println("Accepted connection from " + clientAddress.getHostAddress() + ".");

        } catch (Exception e) {
            System.out.println("Failed to accept new client.");
            e.printStackTrace();

        }
    }

    private void doRead(SelectionKey sk) {
        SocketChannel channel = (SocketChannel) sk.channel();
        //我们准备8K的缓冲区读取数据，所有读取的数据存放在变量bb中。
        ByteBuffer bb = ByteBuffer.allocate(8192);
        int len;
        try {
            len = channel.read(bb);
            if (len < 0) {
                disconnect(sk);
                return;

            }
        } catch (Exception e) {
            System.out.println("Failed to read from client.");
            e.printStackTrace();
            disconnect(sk);
            return;

        }
        //读取完成后，重置缓冲区，为数据处理做准备
        bb.flip();
        //线程池进行任务处理
        tp.execute(new HandleMsg(sk, bb));
    }


    /**
     * 函数doWrite()也接收一个SelectionKey，当然针对一个客户端来说，
     * 这个SelectionKey实例和doRead()拿到的SelectionKey是同一个。
     * 因此，通过SelectionKey我们就可以在这两个操作中共享EchoClient实例
     * @param sk
     */
    private void doWrite(SelectionKey sk) {
        SocketChannel channel = (SocketChannel) sk.channel();
        EchoClient echoClient = (EchoClient) sk.attachment();
        LinkedList<ByteBuffer> outq = echoClient.getOutputQueue();
        //获得列表顶部元素，准备写回客户端
        ByteBuffer bb = outq.getLast();
        try {
            //进行写回操作
            int len = channel.write(bb);
            if (len == -1) {
                disconnect(sk);
                return;

            }
            if (bb.remaining() == 0) {
                // The buffer was completely written, remove it.
                //如果全部发送完成，则移除这个缓存对象
                outq.removeLast();

            }
        } catch (Exception e) {
            System.out.println("Failed to write to client.");
            e.printStackTrace();
            disconnect(sk);

        }
        //在doWrite()中最重要的，也是最容易被忽略的是在全部数据发送完成后（也就是outq的长度为0），需要将写事件（OP_WRITE）从感兴趣的操作中移除。
        // 如果不这么做，每次Channel准备好写时，都会来执行doWrite()方法。而实际上，你又无数据可写，这显然是不合理的。因此，这个操作很重要。
        if (outq.size() == 0) {
            sk.interestOps(SelectionKey.OP_READ);

        }
    }


    class HandleMsg implements Runnable {
        SelectionKey sk;
        ByteBuffer bb;

        public HandleMsg(SelectionKey sk, ByteBuffer bb) {
            this.sk = sk;
            this.bb = bb;

        }

        @Override
        public void run() {
            EchoClient echoClient = (EchoClient) sk.attachment();
            //简单地将接收到的数据压入EchoClient的队列。如果需要处理业务逻辑，就可以在这里进行处理。
            echoClient.enqueue(bb);
            //重新注册感兴趣的消息事件，将写操作（OP_WRITE）也作为感兴趣的事件进行提交,??
            sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            //强迫selector立即返回
            selector.wakeup();

        }
    }

    /**
     * EchoClient的定义很简单，它封装了一个队列，保存在需要回复给这个客户端的所有信息，
     * 这样，再进行回复时，只要从outq对象中弹出元素即可。
     */
    class EchoClient {
        private LinkedList<ByteBuffer> outq;

        EchoClient() {
            outq = new LinkedList<ByteBuffer>();
        }

        public LinkedList<ByteBuffer> getOutputQueue() {
            return outq;
        }

        public void enqueue(ByteBuffer bb) {
            outq.addFirst(bb);
        }
    }

    private void disconnect(SelectionKey sk) {
        sk.cancel();
    }


    /**
     * 启动服务
     * @param args
     */
    public static void main(String[] args) throws Exception {
        NioEchoServer nioEchoServer = new NioEchoServer();
        nioEchoServer.startServer();
    }
}
