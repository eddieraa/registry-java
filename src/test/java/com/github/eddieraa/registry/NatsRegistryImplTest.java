package com.github.eddieraa.registry;
import org.junit.Test;

import io.nats.client.Connection;
import io.nats.client.Nats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        assertEquals("httptest", s.name);
        json = "{\"t\":{\"Duration\":20000}}";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        assertEquals(20000, s.timestamp.duration);
        assertNull(s.name);
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
        assertEquals(20000, s.timestamp.duration);
        json = "{\"t\":{\"duration\":20000},\"add\":\"127.0.0.1:34465\",\"name\":\"httptest\", \"kv\":{} } ";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        json = "{\"t\":{\"duration\":20000}, \"kv\":{\"toto\":\"titi\", \"node\":\"GEN\"} ,\"add\":\"127.0.0.1:34465\",\"name\":\"httptest\"} ";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);
        assertEquals(s.kv.get("toto"), "titi");
        assertEquals(s.kv.get("node"), "GEN");
        s.kv.put("json", "ok");
        String sString = gson.toJson(s);
        assertNotNull(sString);
        json = "{\"t\":{\"duration\":20000}, \"kv\":{\"toto\":\"titi\", \"node\":\"GEN\"} ,\"add\":\"127.0.0.1:34465\",\"name\":\"httptest\", \"xxx\":\"yyy\", \"obj1\":{\"x\":3} } ";
        s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        //Service s = NatsRegistryImpl.parse(gson, json.getBytes(NatsRegistryImpl.UTF8));
        assertNotNull(s);

    }

    @Test
    public void testLoadBalancer() {
        List<Service> list = new ArrayList<>();
        LoadBalanceFilter filter = new LoadBalanceFilter();
        String add1 = "127.0.0.1:4431";
        String add2 = "127.0.0.1:4432";
        String add3 = "127.0.0.1:4433";
        list.add(new Service.Builder("test", add1).build()) ;
        assertEquals(add1, filter.filter(list).get(0).address);
        assertEquals(add1, filter.filter(list).get(0).address);

        list.add(new Service.Builder("test", add2).build()) ;
        filter = new LoadBalanceFilter();
        assertEquals(add1, filter.filter(list).get(0).address);
        assertEquals(add2, filter.filter(list).get(0).address);
        assertEquals(add1, filter.filter(list).get(0).address);
        assertEquals(add2, filter.filter(list).get(0).address);

        list.add(new Service.Builder("test", add3).build()) ;
        filter = new LoadBalanceFilter();
        assertEquals(add1, filter.filter(list).get(0).address);
        assertEquals(add2, filter.filter(list).get(0).address);
        assertEquals(add3, filter.filter(list).get(0).address);
        assertEquals(add1, filter.filter(list).get(0).address);


    }

    @Test
    public void test() throws IOException, InterruptedException, RegistryException {
        Connection conn = Nats.connect();
        assertNotNull(conn);
        Registry reg = RegistryFactory.newNatsRegistry(conn, new Options.Builder().addFilter(new LoadBalanceFilter()).build());
        assertNotNull(reg);
        reg.register(new Service.Builder("java-test", "localhost:5665").build());
        reg.observe("httptest");
        for (int n=0;n<100;n++) {
            Service s = reg.getService("httptest");
            assertNotNull(s);
            System.out.println(s.name+" "+s.address);
        }
        reg.close();
        
    }
    
}
