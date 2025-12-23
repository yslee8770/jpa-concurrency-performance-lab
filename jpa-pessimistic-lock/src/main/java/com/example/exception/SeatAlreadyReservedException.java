package com.example.exception;

public class SeatAlreadyReservedException extends RuntimeException {
  public SeatAlreadyReservedException(Long seatId) {
    super("Seat already reserved. seatId=" + seatId);
  }
}
