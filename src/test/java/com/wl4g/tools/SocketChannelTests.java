package com.wl4g.tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class SocketChannelTests {

    public static void main(String[] args) throws IOException {
        // org.apache.hadoop.hbase.ipc.RpcClientImpl;
        // Socket s = javax.net.SocketFactory.getDefault().createSocket();
        // System.out.println(s.getLocalSocketAddress());
        Socket s = SocketChannel.open().socket();
        s.connect(new InetSocketAddress("127.0.0.1", 16000));
        System.out.println(s.getInputStream());
    }
}
