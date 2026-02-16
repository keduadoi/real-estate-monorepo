package com.realestate.price.exception;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(Long propertyId) {
        super("Price not found for property: " + propertyId);
    }

    public PriceNotFoundException(String message) {
        super(message);
    }
}
