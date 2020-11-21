package com.github.eddieraa.registry;

import java.util.ArrayList;
import java.util.List;

public class Options {
    String mainTopic = "registry";
    long registeredInterval = 2000;
    long readTimeout = 500;// milliseconds
    List<Filter> filters = new ArrayList<>();
    float dueDurationFactor = 1.5f;

    public String getMainTopic() {
        return mainTopic;
    }

    public void setMainTopic(String mainTopic) {
        this.mainTopic = mainTopic;
    }

    public long getRegisteredInterval() {
        return registeredInterval;
    }

    public void setRegisteredInterval(long registeredInterval) {
        this.registeredInterval = registeredInterval;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public void addFilter(Filter f) {
        this.filters.add(f);
    }

    public static class Builder {
        final Options opts = new Options(); 
        public Builder setMainTopic(String mainTopic) {
            this.opts.mainTopic = mainTopic;
            return this;
        }    
        public Builder withReadTimeout(long timeout) {
            this.opts.readTimeout = timeout;
            return this;
        }  
        public Builder withRegisteredInterval(long interval) {
            this.opts.registeredInterval = interval;
            return this;
        }
        public Builder addFilter(Filter filter) {
            this.opts.filters.add(filter);
            return this;
        }

        public Options build() {
            return opts;
        }
    }
    
}
