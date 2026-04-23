package com.smartcampus.exception;
/**
 *
 * @author winil
 */

public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
