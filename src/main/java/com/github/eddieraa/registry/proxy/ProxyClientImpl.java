package com.github.eddieraa.registry.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;

import com.github.eddieraa.registry.Registry;
import com.github.eddieraa.registry.RegistryException;
import com.github.eddieraa.registry.Service;

public class ProxyClientImpl implements ProxyClient {
    final Registry registry;
    String proxyName = "proxy";
    static Logger log = Logger.getLogger(ProxyClientImpl.class.getName());

    public ProxyClientImpl(com.github.eddieraa.registry.Registry registry) {
        this.registry = registry;
    }

    Socket getSocket(String name) throws IOException {
        Service service = null;
        try {
            service = registry.getService(name);
        } catch (Exception e) {
            log.log(Level.FINE, "Service {0} not found {1}",new String[]{name, e.getMessage()});
        }
        if (service == null) {
            return getSocketFromProxy(name);
        }
        return socket(service.getAddress());
    }

    private Socket socket(String address) throws UnknownHostException, IOException {
        String[] toks =  address.split(":");
        String host = toks[0];                              
        int port = Integer.parseInt(toks[1]);
        return new Socket(host, port);
    }

    Socket getSocketFromProxy(String name) throws IOException {
        Service service;
        try {
            service = registry.getService(proxyName);
        } catch (RegistryException e) {
            throw new IOException("network is null", e);
        }
        if (service == null) {
            throw new UnknownHostException("proxy service "+proxyName+" not found");
        }
        if (service.getNetwork() == null) {
            throw new IOException("network is null");
        }

        Socket sock = socket(service.getAddress());
        OutputStream out = sock.getOutputStream();
        StringBuilder b = new StringBuilder();
        b.append("service ").append(name);
        b.append('\r');
        out.write(b.toString().getBytes());
        return sock;
    }

    @Override
    public SocketFactory getSocketFactory(String name) throws RegistryException {
        if (registry == null) {
            throw new RegistryException("Registry server is not initialised");
        }
        registry.observe(name);

        return new SocketFactory() {

            @Override
            public Socket createSocket(String arg0, int arg1) throws IOException {
                return getSocket(name);
            }

            @Override
            public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
                return getSocket(name);
            }

            @Override
            public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
                return getSocket(name);
            }

            @Override
            public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                    throws IOException {
                return getSocket(name);
            }

        };

    }

}
