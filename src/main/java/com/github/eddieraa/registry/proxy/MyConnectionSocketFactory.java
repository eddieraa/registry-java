package com.github.eddieraa.registry.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

public class MyConnectionSocketFactory implements ConnectionSocketFactory {
    final SocketFactory socketFactory;

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        
        return socketFactory.createSocket("fake",0);
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress,
            InetSocketAddress localAddress, HttpContext context) throws IOException {
        return socketFactory.createSocket("fake",0);
    }

    public MyConnectionSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }
    
}
