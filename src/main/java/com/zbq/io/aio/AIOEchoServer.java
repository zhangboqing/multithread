package com.zbq.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by zhangboqing on 2017/6/6.
 */
public class AIOEchoServer {

    public final static int PORT = 8000;
    private AsynchronousServerSocketChannel server;

    public AIOEchoServer() throws IOException {
        //绑定了8000端口为服务器端口，并使用AsynchronousServerSocketChannel异步Channel作为服务器，变量名为server
        server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(PORT));
    }

    /**
     * 开启服务器
     * <p>
     * 这个方法除了打印语句外，只调用了一个函数server.accept()
     * 看到的那一大堆代码只是这个函数的参数。
     * AsynchronousServerSocketChannel.accept()方法会立即返回，它并不会真的去等待客户端的到来。
     *
     * 在这里使用的accept()方法的签名为：public final <A> void accept(A attachment,CompletionHandler<AsynchronousSocketChannel,? super A> handler)
     * 它的第一个参数是一个附件，可以是任意类型，作用是让当前线程和后续的回调方法可以共享信息，它会在后续调用中，传递给handler。
     * 它的第二个参数是CompletionHandler接口。
     * 这个接口有两个方法：
     *  void completed(V result, A attachment);
     *  void failed(Throwable exc, A attachment);
     *  这两个方法分别在异步操作accept()成功或者失败时被回调。
     *
     *  因此AsynchronousServerSocketChannel.accept()实际上做了两件事，
     *  第一是发起accept请求，告诉系统可以开始监听端口了。
     *  第二，注册Com-pletionHandler实例，告诉系统，一旦有客户端前来连接，如果成功连接，就去执行Completion-Handler.completed()方法；
     *  如果连接失败，就去执行CompletionHandler.failed()方法。所以，server.accept()方法不会阻塞，它会立即返回。
     *
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public void start() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Server listen on " + PORT);
        //注册事件和事件完成后的处理器
        server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            final ByteBuffer buffer = ByteBuffer.allocate(1024);

            //当completed()被执行时，意味着已经有客户端成功连接了
            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                System.out.println(Thread.currentThread().getName());
                Future<Integer> writeResult = null;
                try {
                    buffer.clear();
                    //使用read()方法读取客户的数据。这里要注意，Asyn-chronousSocketChannel.read()方法也是异步的，
                    //换句话说它不会等待读取完成了再返回，而是立即返回，返回的结果是一个Future，因此这里就是Future模式的典型应用
                    //直接调用Future.get()方法，进行等待，将这个异步方法变成了同步方法,执行完成后，数据读取就已经完成了
                    result.read(buffer).get(100, TimeUnit.SECONDS);
                    buffer.flip();
                    //将数据回写给客户端。
                    // 这里调用的是AsynchronousSocketChannel.write()方法。这个方法不会等待数据全部写完，也是立即返回的。同样，它返回的也是Future对象
                    writeResult = result.write(buffer);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();

                } catch (TimeoutException e) {
                    e.printStackTrace();

                } finally {
                    try {
                        //服务器进行下一个客户端连接的准备。同时关闭当前正在处理的客户端连接。
                        // 但在关闭之前，得先确保之前的write()操作已经完成，因此，使用Future.get()方法进行等待
                        server.accept(null, this);
                        writeResult.get();
                        result.close();

                    } catch (Exception e) {
                        System.out.println(e.toString());

                    }
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                System.out.println("failed: " + exc);

            }
        });
    }


    public static void main(String args[]) throws Exception {
        //调用start()方法开启服务器
        //由于start()方法里使用的都是异步方法，因此它会马上返回，它并不像阻塞方法那样会进行等待
        new AIOEchoServer().start();
        // 主线程可以继续自己的行为
        //如果想让程序驻守执行，等待语句是必需的。
        // 否则，在start()方法结束后，不等客户端到来，程序已经运行完成，主线程就将退出。
        while (true) {
            Thread.sleep(1000);
        }
    }

}
