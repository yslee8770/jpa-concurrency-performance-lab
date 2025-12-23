package com.example.support;

import com.example.repository.ReservationRepository;
import com.example.repository.ReservationSeatRepository;
import com.example.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DbCleaner {

    private final ReservationSeatRepository reservationSeatRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public void clearAll() {
        reservationSeatRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
    }
}
