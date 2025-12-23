package com.example.service;


import com.example.domain.Reservation;
import com.example.domain.ReservationSeat;
import com.example.domain.Seat;
import com.example.repository.ReservationRepository;
import com.example.repository.ReservationSeatRepository;
import com.example.repository.SeatRepository;
import com.example.exception.SeatNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    @Transactional
    public void reserveSeatsWithoutOrdering(List<Long> seatIds) {
        reserveSeatsInternal(seatIds, false);
    }

    @Transactional
    public void reserveSeatsWithOrdering(List<Long> seatIds) {
        reserveSeatsInternal(seatIds, true);
    }

    private void reserveSeatsInternal(List<Long> seatIds, boolean ordered) {
        List<Long> normalized = normalizeSeatIds(seatIds, ordered);

        Reservation reservation = reservationRepository.save(Reservation.create());

        for (Long seatId : normalized) {
            Seat seat = seatRepository.findByIdForUpdate(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));
            seat.reserve(); // 도메인 불변식
            reservationSeatRepository.save(ReservationSeat.of(reservation, seat));
        }
    }

    private static List<Long> normalizeSeatIds(List<Long> seatIds, boolean ordered) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds must not be null/empty");
        }
        // null 방지 + 중복 제거(같은 좌석 두 번 예약 방지)
        List<Long> distinct = seatIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinct.isEmpty()) throw new IllegalArgumentException("seatIds must contain at least one non-null id");

        if (!ordered) return distinct;

        List<Long> sorted = new ArrayList<>(distinct);
        Collections.sort(sorted);
        return sorted;
    }
}