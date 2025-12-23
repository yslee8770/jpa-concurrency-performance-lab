package com.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "reservation_seat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reservation_seat_reservation_seat",
                        columnNames = {"reservation_id", "seat_id"})
        },
        indexes = {
                @Index(name = "ix_reservation_seat_reservation", columnList = "reservation_id"),
                @Index(name = "ix_reservation_seat_seat", columnList = "seat_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSeat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_seat_reservation"))
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_seat_seat"))
    private Seat seat;

    public static ReservationSeat of(Reservation reservation, Seat seat) {
        ReservationSeat rs = new ReservationSeat();
        rs.reservation = reservation;
        rs.seat = seat;
        return rs;
    }
}
