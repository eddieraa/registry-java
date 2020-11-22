# Service Registry java implementation based on pub/sub pattern

[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

This service registry use nats (https://nats.io). No server is required. This implementation used pub/sub message for register/unregister service.


## Feature

* Filtering: Filter could be used for exclude service, load balancing, prioritize some services 
* TTL: the registered services send periodiacaly a message on nats
Client check the ttl.


[golang implentation](../registry)

## Basic usage

```java
//Create a registry with a nats connection
Connection conn = Nats.Connect();
Registry reg = null;
try {
    reg = RegistryFactory.newNatsRegistry(conn);

    //On the service side
    //Register your service
    reg.register(new Service.Builder("myservice","10.10.2.3:6769"));

    //On the client side
    Service myservice = reg.getService("myservice");
    //Do something with address:  myservice.getAddress()



} finally {
    reg.close();
}



```