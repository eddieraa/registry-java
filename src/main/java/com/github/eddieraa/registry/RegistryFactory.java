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
        RegistryFactory.instance = new NatsRegistryImpl(conn);
        return RegistryFactory.instance;
    }
}