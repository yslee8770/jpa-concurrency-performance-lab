package com.example.domain;

import com.example.exception.SeatAlreadyReservedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean reserved;

    public static Seat create() {
        Seat s = new Seat();
        s.reserved = false;
        return s;
    }

    public void reserve() {
        if (this.reserved) {
            throw new SeatAlreadyReservedException(this.id);
        }
        this.reserved = true;
    }
}