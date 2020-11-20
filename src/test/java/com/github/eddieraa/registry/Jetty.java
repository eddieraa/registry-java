package com.github.eddieraa.registry;

import com.github.eddieraa.registry.Registry;
import com.github.eddieraa.registry.RegistryFactory;
import com.github.eddieraa.registry.Service;
import com.github.eddieraa.registry.proxy.ProxyRegistryServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class Jetty {
    static void setProperties() {
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime","true");
        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime","true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http","ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire","ERROR");

    }
    
    public static void main(String[] args) {
        int port = 8080;
        Server server = new Server(port);
        Registry reg = null;
        Connection conn = null;
        setProperties();
        try {

            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);
            conn = Nats.connect();
            reg = RegistryFactory.newNaRegistry(conn);
            reg.register(new Service.Builder("testproxy", "localhost:"+port).build());
           

            ServletHolder holder = handler.addServletWithMapping(ProxyRegistryServlet.class, "/proxy/*");
            holder.setInitParameter("targetUri", "http://localhost:444" );
            holder.setInitParameter("log", "false");
            holder.setInitParameter(ProxyRegistryServlet.P_SERVICENAME, "httptest");
            holder.setInitParameter(ProxyRegistryServlet.P_MAXCONNECTIONS, "5");
            holder.setInitParameter(ProxyRegistryServlet.P_PRESERVEHOST, "true");
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reg!=null) reg.close();
                if (conn!=null) conn.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            
        }

    }
}
