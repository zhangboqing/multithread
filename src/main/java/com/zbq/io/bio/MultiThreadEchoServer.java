package com.zbq.io.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhangboqing on 2017/6/5.
 * 服务器会为每一个客户端连接启用一个线程，这个新的线程将全心全意为这个客户端服务。
 * 同时，为了接受客户端连接，服务器还会额外使用一个派发线程。
 *
 * 这就是一个支持多线程的服务端的核心内容。
 * 它的特点是，在相同可支持的线程范围内，可以尽量多地支持客户端的数量，同时和单线程服务器相比，它也可以更好地使用多核CPU。
 *
 * 存在的问题：这种模式的一个重大弱点——那就是它倾向于让CPU进行IO等待
 */
public class MultiThreadEchoServer {

    private static ExecutorService tp = Executors.newCachedThreadPool();

    static class HandleMsg implements Runnable {
        Socket clientSocket;

        public HandleMsg(Socket clientSocket) {
            this.clientSocket = clientSocket;

        }

        public void run() {
            BufferedReader is = null;
            PrintWriter os = null;
            try {
                is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                os = new PrintWriter(clientSocket.getOutputStream(), true);
                // 从InputStream当中读取客户端所发送的数据
                String inputLine = null;
                long b = System.currentTimeMillis();
                while ((inputLine = is.readLine()) != null) {
                    os.println(inputLine);

                }
                long e = System.currentTimeMillis();
                System.out.println("spend:" + (e - b) + "ms");

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                try {
                    if (is != null) is.close();
                    if (os != null) os.close();
                    clientSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        }
    }

    public static void main(String args[]) {
        ServerSocket echoServer = null;
        Socket clientSocket = null;
        try {
            echoServer = new ServerSocket(8000);

        } catch (IOException e) {
            System.out.println(e);

        }
        while (true) {
            try {
                clientSocket = echoServer.accept();
                System.out.println(clientSocket.getRemoteSocketAddress() + " connect!");
                tp.execute(new HandleMsg(clientSocket));

            } catch (IOException e) {
                System.out.println(e);

            }
        }
    }

}
