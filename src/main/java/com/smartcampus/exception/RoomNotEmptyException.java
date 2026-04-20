package com.smartcampus.exception;

/**
 * Thrown when a DELETE /rooms/{id} is attempted but the room still
 * has one or more sensors assigned to it.
 *
 * The ExceptionMapper (Day 4) will convert this into HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' still has sensors attached. "
                + "Remove all sensors before deleting the room.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
