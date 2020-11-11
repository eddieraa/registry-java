package com.github.eddieraa.registry;

import java.util.List;

public interface Filter {
    public List<Service> filter(List<Service> services );
}
