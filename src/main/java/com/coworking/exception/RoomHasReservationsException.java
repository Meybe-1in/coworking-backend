package com.coworking.exception;

public class RoomHasReservationsException extends RuntimeException {
    public RoomHasReservationsException() {
        super("La sala tiene reservas asociadas");
    }
}
