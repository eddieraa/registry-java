package com.github.eddieraa.registry;

import java.util.Map;

import com.google.gson.annotations.JsonAdapter;

@JsonAdapter(ServiceJsonAdapter.class)
public class Service {
    
    String name;
    String network;
    String address;
    String url;
    String version;
    String host;
    Timestamps timestamp;
    Map<String,String> kv;

    public String getAddress() {
        return address;
    }
    public String getHost() {
        return host;
    }
    public String getName() {
        return name;
    }
    public String getNetwork() {
        return network;
    }
    public String getUrl() {
        return url;
    }
    public String getVersion() {
        return version;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setNetwork(String network) {
        this.network = network;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getKv() {
        return kv;
    }

    public void setKv(Map<String, String> kv) {
        this.kv = kv;
    }

    public int getPort() {
        String[] split = this.address.split(":");
        return Integer.parseInt(split[1]);
    }

    class Timestamps {
        long registered;
        int duration;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(name).append(" (").append(address).append(") ").append(host);
        return b.toString();
    }



    public static class Builder {
        final String name;
        String network;
        final String address;
        String url;
        String version;
        String host;
        public Builder(String name, String address) {
            this.name = name;
            this.address = address;
        }
        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }
        public Builder setHost(String host) {
            this.host = host;
            return this;
        }
        public Builder setNetwork(String network){
            this.network = network;
            return this;
        }
        public Service build() {
            Service s = new Service();
            s.name = name;
            s.address = address;
            s.host = host;
            s.network = network;
            s.url = url;
            s.version = version;
            return s;
        }

    }
}
