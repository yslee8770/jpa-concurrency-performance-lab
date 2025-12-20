package com.example.jpa_concurrency_performance_lab.domain.order;

import com.example.jpa_concurrency_performance_lab.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "order_line",
        indexes = {
                @Index(name = "idx_order_line_order_id", columnList = "purchase_order_id"),
                @Index(name = "idx_order_line_sku", columnList = "sku")
        }
)
public class OrderLine extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_line_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder order;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderLine() {}

    public static OrderLine create(PurchaseOrder order, String sku, int unitPrice, int quantity) {
        OrderLine line = new OrderLine();
        line.order = order;
        line.sku = sku;
        line.unitPrice = unitPrice;
        line.quantity = quantity;
        return line;
    }
}
