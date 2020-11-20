package com.github.eddieraa.registry.proxy;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.github.eddieraa.registry.Filter;
import com.github.eddieraa.registry.Service;

public class ProxyFilter implements Filter {
    String hostname = null;
    public ProxyFilter() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (Exception e) {
            //TODO: handle exception
        }
    }
    @Override
    public List<Service> filter(List<Service> services) {
        List<Service> lst = new ArrayList<>(services.size());
        for (Service s : services) {
            if (s.getAddress().startsWith("proxy:")) {
                //is a proxy add
            } else if (s.getHost()!=null && s.getHost().equals(hostname)) {
                lst.add(s);
            } else if (s.getAddress().startsWith("localhost:") || s.getAddress().startsWith("127.0.0.1")) {
                //address listen lookback on remote host
            } else {
                lst.add(s);
            }
            
        }
        return lst;
    }
}
