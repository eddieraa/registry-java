package com.github.eddieraa.registry;
import org.junit.Test;

import io.nats.client.Connection;
import io.nats.client.Nats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NatsRegistryImplTest {
    @Test
    public void testParse() throws IOException, InterruptedException, RegistryException {
        GsonBuilder builder = new GsonBuilder();
  
        builder.registerTypeAdapter(Service.class, new ServiceJsonAdapter());
  
        Gson gson = builder.create();
  
        String json = "{\"add\":\"127.0.0.1:34465\",\"name\":\"httptest\"}";
        Service s = gson.fromJson(json, Service.class);
        assertNotNull(s);
        json = "{\"t\":{\"Duration\":20000}}";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        json = "{\"add\":\"127.0.0.1:34465\",\"name\":\"httptest\",\"t\":{\"Registered\":1605016819305899785,\"Duration\":20000}}";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        json = "{\"t\":{\"duration\":20000}}";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        json = "{\"t\":{\"duration\":20000},\"add\":\"127.0.0.1:34465\",\"name\":\"httptest\"}";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        assertEquals("127.0.0.1:34465", s.address);
 
    }

    @Test
    public void test() throws IOException, InterruptedException, RegistryException {
        Connection conn = Nats.connect();
        assertNotNull(conn);
        Registry reg = RegistryFactory.newNaRegistry(conn);
        assertNotNull(reg);
        Service s = reg.getService("httptest");
        assertNotNull(s);
    }
    
}
