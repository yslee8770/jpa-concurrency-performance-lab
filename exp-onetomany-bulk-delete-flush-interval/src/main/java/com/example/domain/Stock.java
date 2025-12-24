package com.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "stock",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stock_product_id", columnNames = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Stock extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Builder
    private Stock(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public static Stock create(Product product, int quantity) {
        Stock stock = builder()
                .product(product)
                .quantity(quantity)
                .build();
        stock.attachProduct(product);
        return stock;
    }

    void attachProduct(Product product) {
        this.product = product;
    }

    public void decrease(int amount) {
        if (this.quantity - amount < 0) {
            throw new IllegalStateException("OUT_OF_STOCK");
        }
        this.quantity -= amount;
    }

    public void increase(int amount) {
        this.quantity += amount;
    }
}

