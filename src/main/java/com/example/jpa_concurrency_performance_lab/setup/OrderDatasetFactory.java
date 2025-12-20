package com.example.jpa_concurrency_performance_lab.setup;


import com.example.jpa_concurrency_performance_lab.domain.order.OrderLineDraft;

import java.util.ArrayList;
import java.util.List;

public final class OrderDatasetFactory {
    private OrderDatasetFactory() {}

    public static List<OrderLineDraft> drafts(int size) {
        List<OrderLineDraft> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String sku = "SKU-" + (i % 200);
            int price = 1000 + (i % 50) * 10;
            int qty = 1 + (i % 3);
            list.add(new OrderLineDraft(sku, price, qty));
        }
        return list;
    }
}
