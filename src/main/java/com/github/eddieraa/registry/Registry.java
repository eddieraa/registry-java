package com.github.eddieraa.registry;

import java.util.List;

public interface Registry {
    public void register(Service service) throws RegistryException;
    public Service getService(String name) throws RegistryException;
    public List<Service> getServices(String name) throws RegistryException;
    public void unregister(Service service) throws RegistryException;
    public void observe(String name);
}
