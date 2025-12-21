package com.example.domain;

import com.example.domain.exception.OutOfStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stock")
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 낙관적 락 핵심: version 컬럼
     */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private long quantity;

    private Stock(long quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public static Stock of(long quantity) {
        return new Stock(quantity);
    }

    public void decrease(long amount) {
        validateAmount(amount);
        if (this.quantity < amount) {
            throw new OutOfStockException(this.id, this.quantity, amount);
        }
        this.quantity -= amount;
    }

    public void increase(long amount) {
        validateAmount(amount);
        this.quantity += amount;
    }

    private static void validateQuantity(long quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
    }

    private static void validateAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }
}
