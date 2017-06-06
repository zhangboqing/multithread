package com.zbq.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Created by zhangboqing on 2017/6/6.
 */
public class AIOEchoClient {

    public static void main(String[] args) throws Exception {
        //1.打开Asynchronous-SocketChannel通道
        final AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        //2.让客户端去连接指定的服务器，并注册了一系列事件
        client.connect(new InetSocketAddress("localhost", 8000), null, new CompletionHandler<Void, Object>() {


            //连接成功回调
            @Override
            public void completed(Void result, Object attachment) {
                //进行数据写入，向服务端发送数据
                //这个过程也是异步的，会很快返回。写入完成后，会通知回调接口CompletionHandler<Integer,Object>
                client.write(ByteBuffer.wrap("Hello!".getBytes())
                        , null, new CompletionHandler<Integer, Object>() {

                            @Override
                            public void completed(Integer result, Object attachment) {
                                try {
                                    //准备进行数据读取，从服务端读取回写的数据
                                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                                    //read()函数也是立即返回的，成功读取所有数据后，会回调CompletionHandler<Integer,ByteBuffer>接口
                                    client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {

                                        @Override
                                        public void completed(Integer result, ByteBuffer buffer) {
                                            //打印接收到的数据
                                            buffer.flip();
                                            System.out.println(new String(buffer.array()));
                                            try {
                                                client.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();

                                            }
                                        }

                                        @Override
                                        public void failed(Throwable exc, ByteBuffer attachment) {

                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();

                                }
                            }

                            @Override
                            public void failed(Throwable exc, Object attachment) {
                            }
                        });
            }

            //连接失败回调
            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        });
        //3.让线程进行等待
        //由于主线程马上结束，这里等待上述处理全部完成
        Thread.sleep(1000);

    }
}
