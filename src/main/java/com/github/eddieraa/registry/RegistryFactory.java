package com.github.eddieraa.registry;

import io.nats.client.Connection;


public class RegistryFactory {
    static Registry instance;
    private RegistryFactory(){
    }
    
    public static Registry instance() {
        return instance;
    }
    public static Registry newNaRegistry(Connection conn) throws RegistryException {
        return new NatsRegistryImpl(conn, new Options());
    }
    public static Registry newNaRegistry(Connection conn, Options opts) throws RegistryException {
        RegistryFactory.instance = new NatsRegistryImpl(conn, opts);
        return RegistryFactory.instance;
    }
    
}