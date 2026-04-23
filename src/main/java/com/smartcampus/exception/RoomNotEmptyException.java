package com.smartcampus.exception;

/**
 * This exception is used when someone tries to delete a room that still has sensors in it.
 * We don't allow that because deleting the room would leave orphaned sensors in the system.
 *
 * The ExceptionMapper will catch this and send back a 409 Conflict response.
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
