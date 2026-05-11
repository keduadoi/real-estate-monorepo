package com.realestate.comments.exception;

public class PropertyNotFoundException extends RuntimeException {
    public PropertyNotFoundException(Long id) {
        super("Property not found: " + id);
    }
}
