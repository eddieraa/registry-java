package com.github.eddieraa.registry;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Subscription;

public class NatsRegistryImpl implements Registry {
    final Connection conn;
    static final Charset UTF8 = StandardCharsets.UTF_8;
    static final String TOPIC_REGISTER = "register";
    static final String TOPIC_UNREGISTER = "unregister";
    static final String TOPIC_PING = "ping";

    // Parameters
    String mainTopic = "registry";
    long registeredInterval = 2000;
    long readTimeout = 50000;// milliseconds

    Map<String, Subscription> subscriptions = new HashMap<>();
    Map<String, Service> registeredServices = new HashMap<>();
    final Dispatcher dispatcher;
    static Logger log = Logger.getLogger(NatsRegistryImpl.class.getName());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    boolean alive = true;
    Map<String, Observe> observers = new HashMap<>();
    Map<String, Map<String, Service>> m = new HashMap<>();
    List<Filter> filters = new ArrayList<>();
    final Gson gson;// Thread safe

    protected NatsRegistryImpl(Connection conn) {
        this.conn = conn;
        dispatcher = createDispatcher();
        runThread();
        log.setLevel(Level.FINEST);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Service.class, new ServiceJsonAdapter());
        gson = builder.create();
    }

    private void runThread() {
        executor.submit(() -> {
            while (alive) {
                for (Service s : registeredServices.values()) {
                    pubRegister(s);
                }
                try {
                    if (alive)
                        Thread.sleep(registeredInterval);
                } catch (InterruptedException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                    Thread.currentThread().interrupt();
                    ;
                }
            }
        });
    }

    private Dispatcher createDispatcher() {
        return conn.createDispatcher((msg) -> {
            log.info("Receive new message " + msg.getSubject());
        });
    }

    String buildMessage(String... names) {
        StringBuilder b = new StringBuilder(mainTopic);
        for (String n : names) {
            b.append('/').append(n);
        }
        return b.toString();
    }

    Service udpateService(Service service) {
        if (service.timestamp == null) {
            service.timestamp = service.new Timestamps();
            service.timestamp.duration = 5000;
            service.timestamp.registered = System.currentTimeMillis();
        }
        return service;
    }

    void pubRegister(Service service) {
        byte[] data = gson.toJson(udpateService(service)).getBytes(UTF8);
        conn.publish(buildMessage(TOPIC_REGISTER, service.name), data);
    }

    static Service parse(Gson gson, byte[] data) {
        Service s = null;
        try {
            s = gson.fromJson(new String(data, UTF8), Service.class);
        } catch (Exception e) {
            log.info("data is " + new String(data, UTF8));
            log.log(Level.SEVERE, "Unable to parse data ", e);
        }

        return s;
    }

    Service parse(byte[] data) {
        return parse(gson, data);
    }

    void subRegister(String name) {
        dispatcher.subscribe(buildMessage(TOPIC_REGISTER, name), msg -> {
            log.info("receive new message from " + msg.getSubject());
            Service s = parse(msg.getData());
            Observe o = observers.get(name);
            if (o != null && o.call != null) {
                o.call.call(s);
            }
            Map<String, Service> services = m.get(name);
            if (services == null) {
                services = new HashMap<>();
                m.put(name, services);
            }
            Service old = services.get(name + s.address);
            if (old == null) {
                services.put(name + s.address, old);
            } else {
                old.timestamp = s.timestamp;
            }
        });
    }

    void subUnregister(String name) {
        dispatcher.subscribe(buildMessage(TOPIC_UNREGISTER, name), msg -> {
            log.info(msg.toString());
            Service s = gson.fromJson(new String(msg.getData(), UTF8), Service.class);
            Map<String, Service> services = m.get(name);
            if (services == null) {
                return;
            }
            if (services.containsKey(name + s.address)) {
                services.remove(name + s.address);
            }
        });
    }

    @Override
    public void register(Service service) throws RegistryException {
        dispatcher.subscribe(buildMessage("ping", service.getName()), (msg) -> {
            udpateService(service);
            conn.publish(msg.getReplyTo(), gson.toJson(service).getBytes(UTF8));
        });
        registeredServices.put(service.name + service.address, service);
        pubRegister(service);
    }

    @Override
    public Service getService(String name) throws RegistryException {
        List<Service> services = getServices(name);
        if (services.isEmpty())
            return null;
        return services.get(0);
    }

    private List<Service> chainFilters(Map<String, Service> services) {
        List<Service> res = new ArrayList<>(services.values());
        for (Filter f : filters) {
            res = f.filter(res);
        }
        return res;
    }

    @Override
    public List<Service> getServices(String name) throws RegistryException {
        Map<String, Service> services = m.get(name);
        if (m.containsKey(name)) {
            return chainFilters(services);
        }
        Observe observe = new Observe();
        observers.put(name, observe);
        BlockingDeque<Service> queue = new LinkedBlockingDeque<>();
        Service service = null;
        if (!filters.isEmpty()) {
            observe.call = s -> {
                Map<String, Service> _m = new HashMap<>(1);
                _m.put(s.name, s);
                List<Service> res = chainFilters(_m);
                if (!res.isEmpty()) {
                    queue.add(s);
                }
            };

        } else {
            observe.call = s -> queue.add(s);
        }
        observe(name);
        ping(name);
        try {
            service = queue.poll(readTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("could not find service " + name);
            throw new RegistryException(e);
        }
        observe.call = null;
        return Collections.singletonList(service);

    }

    @Override
    public void unregister(Service service) throws RegistryException {
        byte[] data = gson.toJson(service).getBytes(UTF8);
        conn.publish(buildMessage(TOPIC_UNREGISTER, service.name), data);
        registeredServices.remove(service.name + service.address);
    }

    void ping(String name) {
        conn.publish(buildMessage(TOPIC_PING, name), buildMessage(TOPIC_REGISTER, name), null);
    }

    @Override
    public void observe(String name) {
        Observe obs = observers.get(name);
        if (obs != null && obs.call == null) {
            return;
        } else if (obs == null) {
            obs = new Observe();
        }
        observers.put(name, obs);
        subRegister(name);
        subUnregister(name);
    }

    class Observe {
        ObserveCall call;
    }

    interface ObserveCall {
        void call(Service s);
    }
}