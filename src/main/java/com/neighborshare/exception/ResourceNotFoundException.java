package com.neighborshare.exception;

public class ResourceNotFoundException extends NeighborShareException {
    public ResourceNotFoundException(String resourceName, String identifier) {
        super(resourceName + " not found: " + identifier, "RESOURCE_NOT_FOUND");
    }
}
