package com.github.eddieraa.registry.proxy;

import javax.net.SocketFactory;
import javax.servlet.ServletException;

import com.github.eddieraa.registry.Registry;
import com.github.eddieraa.registry.RegistryException;
import com.github.eddieraa.registry.RegistryFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

public class ProxyRegistryServlet extends ProxyServlet {
    /**
     *
     */
    private static final long serialVersionUID = -4017426447663169295L;

    public static final String P_SERVICENAME = "serviceName";

    protected String serviceName = null;
    protected Registry registry = null;

   
    @Override
    public void init() throws ServletException {
        serviceName = getConfigParam(P_SERVICENAME);
        registry = RegistryFactory.instance();
        //MUST be after the code above
        super.init();
    }

    @Override
    //Override standard implementation
    protected HttpClient createHttpClient() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(buildRequestConfig())
                .setDefaultSocketConfig(buildSocketConfig());

        clientBuilder.setMaxConnTotal(maxConnections);

        if (useSystemProperties)
            clientBuilder = clientBuilder.useSystemProperties();

        return extendsHttpClientBuilder(clientBuilder).build();
    }

    protected HttpClientBuilder extendsHttpClientBuilder(HttpClientBuilder clientBuilder) {
        ProxyClient proxy = new ProxyClientImpl(registry);
        SocketFactory socketFactory;
        try {
            socketFactory = proxy.getSocketFactory(serviceName);
        } catch (RegistryException e) {
           throw new IllegalArgumentException(e);
        }
        org.apache.http.config.Registry<ConnectionSocketFactory> reg = org.apache.http.config.RegistryBuilder
                .<ConnectionSocketFactory>create().register("http", new MyConnectionSocketFactory(socketFactory))
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(reg);
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        clientBuilder.setConnectionManager(connectionManager);
        return clientBuilder;
    }

}
