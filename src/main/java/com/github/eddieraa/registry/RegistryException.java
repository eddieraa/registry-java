package com.github.eddieraa.registry;

public class RegistryException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 9190846976738954801L;
    public RegistryException(Throwable e) {
        super(e);
    }

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    
}
