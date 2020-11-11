package com.github.eddieraa.registry.proxy;

import javax.net.SocketFactory;

import com.github.eddieraa.registry.RegistryException;

public interface ProxyClient {
    public SocketFactory getSocketFactory(String service)  throws RegistryException;
    
}
