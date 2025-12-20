package com.example.jpa_concurrency_performance_lab.domain.order;

import com.example.jpa_concurrency_performance_lab.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String customerKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.CREATED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderLine> lines = new ArrayList<>();

    protected PurchaseOrder() {}

    private PurchaseOrder(String customerKey) {
        this.customerKey = customerKey;
    }

    public static PurchaseOrder create(String customerKey) {
        return new PurchaseOrder(customerKey);
    }

    public void addLine(String sku, int unitPrice, int quantity) {
        lines.add(OrderLine.create(this, sku, unitPrice, quantity));
    }

    /** 실험용: 컬렉션 전체 교체(= orphan 대량 발생) */
    public void replaceAllLines(List<OrderLineDraft> drafts) {
        lines.clear();
        for (OrderLineDraft d : drafts) {
            lines.add(OrderLine.create(this, d.sku(), d.unitPrice(), d.quantity()));
        }
    }

    /** 실험에서 “컬렉션 로딩 여부”를 통제하기 위한 메서드 */
    public int forceLoadLinesCount() {
        return lines.size();
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
