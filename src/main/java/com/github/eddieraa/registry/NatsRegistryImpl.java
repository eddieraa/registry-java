package com.github.eddieraa.registry;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
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
    static Logger log = Logger.getLogger(NatsRegistryImpl.class.getName());
    // Parameters
    final Options opts;
    final Map<String, Subscription> subscriptions = new HashMap<>();
    final Map<String, Service> registeredServices = new HashMap<>();
    final Dispatcher dispatcher;
    final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    });
    final Map<String, Observe> observers = new HashMap<>();
    final Map<String, Map<String, Service>> m = Collections.synchronizedMap(new HashMap<>());
    final Gson gson;// Thread safe

    boolean alive = true;
    boolean paused = false;
 

    protected NatsRegistryImpl(Connection conn, Options opts) {
        this.conn = conn;
        this.opts = opts;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Service.class, new ServiceJsonAdapter());
        gson = builder.create();
        log.setLevel(opts.logLevel);
        dispatcher = createDispatcher();
        runThread();
    }

    protected boolean checkServiceTTL(Service s) {
        if (s.timestamp == null)
            return true;
        long dueTime = s.timestamp.registered + (long) opts.dueDurationFactor * s.timestamp.duration;
        return dueTime > System.currentTimeMillis();
    }

    protected void checkServices() {
        Map<String, Service> toDelete = new HashMap<>();
        for (Map<String, Service> services : m.values()) {
            for (Service s : services.values()) {
                if (!checkServiceTTL(s)) {
                    log.log(Level.INFO, "Service {0} ({1}) is outdated", new String[] { s.name, s.address });
                    toDelete.put(s.name + " " + s.name+s.address, s);
                }
            }
        }
        for (Entry<String, Service> e : toDelete.entrySet()) {
            String[] toks = e.getKey().split(" ");
            Map<String, Service> services = m.get(toks[0]);
            if (services != null) {
                Service s = services.remove(toks[1]);
                if (s != null) {
                    log.log(Level.INFO, "Service {0} deleted ", s);
                }
            }
        }
    }

    private void task() {
        try {
            log.setLevel(Level.FINE);
            log.log(Level.FINE, "Start registryControler with interval of {0}ms" , opts.registeredInterval);

            Thread.currentThread().setName("RegistryControler");
            while (alive) {
                if (!paused) {
                    for (Service s : registeredServices.values()) {
                        pubRegister(s);
                    }
                    checkServices();
                }

                if (alive) {
                    Thread.sleep(opts.registeredInterval);
                }
            }
            log.info("Stop registryControler ");
        } catch (InterruptedException e) {
            log.log(Level.WARNING, e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error in RegistryControler", e);
            e.printStackTrace();
        }
    }
    private void runThread() {
        executor.submit(this::task);
    }

    private Dispatcher createDispatcher() {
        return conn.createDispatcher(msg -> log.info("Receive new message " + msg.getSubject()));
    }

    String buildMessage(String... names) {
        StringBuilder b = new StringBuilder(opts.mainTopic);
        for (String n : names) {
            b.append('.').append(n);
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
        Subscription sub = dispatcher.subscribe(buildMessage(TOPIC_REGISTER, name), msg -> {
            Service s = parse(msg.getData());
            log.fine("rcv "+msg.getSubject() + " (" + s.address + ")");
            Observe o = observers.get(name);
            if (o != null && o.call != null) {
                o.call.call(s);
            }
            Map<String, Service> services = m.get(name);
            if (services == null) {
                services = Collections.synchronizedMap(new HashMap<>());
                m.put(name, services);
            }
            Service old = services.get(name + s.address);
            if (old == null) {
                services.put(name + s.address, s);
            } else {
                old.timestamp = s.timestamp;
            }
        });
        subscriptions.put("reg" + name, sub);
    }

    void subUnregister(String name) {
        Subscription sub = dispatcher.subscribe(buildMessage(TOPIC_UNREGISTER, name), msg -> {
            Service s = parse(msg.getData());
            log.info(msg.getSubject() + " (" + s.address + ")");
            Map<String, Service> services = m.get(name);
            if (services == null) {
                return;
            }
            if (services.containsKey(name + s.address)) {
                services.remove(name + s.address);
            }
        });
        subscriptions.put("unreg" + name, sub);
    }

    @Override
    public void register(Service service) throws RegistryException {
        Subscription sub = dispatcher.subscribe(buildMessage("ping", service.getName()), msg -> {
            udpateService(service);
            conn.publish(msg.getReplyTo(), gson.toJson(service).getBytes(UTF8));
        });
        registeredServices.put(service.name + service.address, service);
        subscriptions.put("ping" + service.name, sub);
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
        for (Filter f : opts.filters) {
            res = f.filter(res);
        }
        return res;
    }

    @Override
    public List<Service> getServices(String name) throws RegistryException {
        Map<String, Service> services = m.get(name);
        if (services != null) {
            return chainFilters(services);
        }
        Observe observe = new Observe();
        observers.put(name, observe);
        BlockingDeque<Service> queue = new LinkedBlockingDeque<>();
        Service service = null;
        if (!opts.filters.isEmpty()) {
            observe.call = s -> {
                Map<String, Service> mServices = new HashMap<>(1);
                mServices.put(s.name, s);
                List<Service> res = chainFilters(mServices);
                if (!res.isEmpty()) {
                    queue.add(s);
                }
            };

        } else {
            observe.call = queue::add;
        }
        observe(name);
        ping(name);
        try {
            service = queue.poll(opts.readTimeout, TimeUnit.MILLISECONDS);
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
        Subscription sub = subscriptions.get("ping" + service.name);
        if (sub != null) {
            dispatcher.unsubscribe(sub);
            subscriptions.remove("ping" + service.name);
        }
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
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
            ping(name);
            obs = new Observe();
        }
        observers.put(name, obs);
        subRegister(name);
        subUnregister(name);
    }

    @Override
    public void close() throws RegistryException {
        alive = false;
        for (Subscription s : subscriptions.values()) {
            dispatcher.unsubscribe(s);
        }
        subscriptions.clear();
        registeredServices.clear();
        observers.clear();
        m.clear();
        executor.shutdown();
        try {
            dispatcher.drain(Duration.ofMillis(400));
        } catch (Exception e) {
            log.warning(e.getMessage());
        }

    }

    class Observe {
        ObserveCall call;
    }

    interface ObserveCall {
        void call(Service s);
    }
}