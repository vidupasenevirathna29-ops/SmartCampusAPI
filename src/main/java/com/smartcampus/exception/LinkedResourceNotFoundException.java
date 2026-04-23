package com.smartcampus.exception;

/**
 * This exception is thrown when you try to link a sensor to a room that doesn't exist.
 * For example, if someone sends a POST /sensors request with a roomId that we can't find,
 * we throw this instead of letting it fail silently.
 *
 * The ExceptionMapper picks this up and returns a 422 Unprocessable Entity response.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " with id '" + resourceId + "' was not found.");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
