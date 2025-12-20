package com.example.jpa_concurrency_performance_lab.setup;

import com.example.jpa_concurrency_performance_lab.domain.product.Product;
import com.example.jpa_concurrency_performance_lab.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateDataSeeder {

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;

    /**
     * 매 테스트마다:
     * - product_id = 1..N
     * - price = basePrice
     * - stock quantity = baseQty (동시성/재고 실험까지 확장 가능)
     */
    @Transactional
    public void reseedProducts(int n, int basePrice, int baseQty) {
        truncateAndResetIdentity();

        List<Product> products = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            products.add(Product.create(
                    "P-" + i,
                    basePrice,
                    "CAT-" + (i % 10),
                    baseQty
            ));
        }

        productRepository.saveAll(products);
        productRepository.flush(); // 바로 실험 가능하도록 DB 반영
    }

    private void truncateAndResetIdentity() {
        // FK 때문에 자식 -> 부모 순서로 DELETE
        jdbcTemplate.execute("DELETE FROM stock");
        jdbcTemplate.execute("DELETE FROM product");

        // IDENTITY 리셋 (H2)
        jdbcTemplate.execute("ALTER TABLE stock ALTER COLUMN stock_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE product ALTER COLUMN product_id RESTART WITH 1");
    }
}
