package com.smartcampus.exception;

/**
 * Thrown when a resource references another resource that does not exist.
 * Primary use-case: POST /sensors with a roomId that does not match any Room.
 *
 * The ExceptionMapper (Day 4) will convert this into HTTP 422 Unprocessable Entity.
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
