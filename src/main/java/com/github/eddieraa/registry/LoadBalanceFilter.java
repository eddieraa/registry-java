package com.github.eddieraa.registry;

import java.util.Collections;
import java.util.List;

public class LoadBalanceFilter implements Filter {
    int index = -1;
    @Override
    public List<Service> filter(List<Service> services) {
        if (services.isEmpty() || services.size()==1) return services;
        if (++index>=services.size()) {
            index = 0;
        }
        return  Collections.singletonList(services.get(index));
    }
    
}
