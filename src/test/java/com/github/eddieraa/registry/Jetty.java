package com.github.eddieraa.registry;


import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.eddieraa.registry.proxy.ProxyFilter;
import com.github.eddieraa.registry.proxy.ProxyRegistryServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class Jetty {
    static Logger log = Logger.getLogger(Jetty.class.getName());
    static void setProperties() {
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime","true");
        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime","true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http","ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire","ERROR");
        System.setProperty("java.util.logging.SimpleFormatter.format","%1$tF %1$tT,%1$tL %4$s %5$s %n");

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
            Options opts = new Options.Builder()
                .addFilter(new ProxyFilter())
                .addFilter(new LoadBalanceFilter())
                .build();
            reg = RegistryFactory.newNatsRegistry(conn,opts);
            reg.register(new Service.Builder("testproxy", "localhost:"+port).build());
           

            ServletHolder holder = handler.addServletWithMapping(ProxyRegistryServlet.class, "/proxy/*");
            holder.setInitParameter("log", "false");
            holder.setInitParameter(ProxyRegistryServlet.P_SERVICENAME, "httptest");
            holder.setInitParameter(ProxyRegistryServlet.P_MAXCONNECTIONS, "5");
            holder.setInitParameter(ProxyRegistryServlet.P_PRESERVEHOST, "true");
            holder.setInitOrder(4);
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reg!=null) reg.close();
                if (conn!=null) conn.close();
            } catch (Exception e) {
                log.log(Level.SEVERE,"Exception in Jetty", e);
            }
            
            
        }

    }
}
