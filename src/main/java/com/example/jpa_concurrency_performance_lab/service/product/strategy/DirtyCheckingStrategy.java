package com.example.jpa_concurrency_performance_lab.service.product.strategy;

import java.util.List;

import com.example.jpa_concurrency_performance_lab.domain.product.Product;
import com.example.jpa_concurrency_performance_lab.dto.UpdateRange;
import com.example.jpa_concurrency_performance_lab.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirtyCheckingStrategy implements BulkUpdateStrategy {

    private static final int PAGE_SIZE = 200;
    private static final int FLUSH_INTERVAL = 200; // 100/500/1000은 다음 실험에서 파라미터화

    private final ProductRepository repository;
    private final EntityManager em;

    @Override
    public String name() {
        return "DIRTY_CHECKING";
    }

    @Override
    @Transactional
    public void execute(UpdateRange range) {
        long total = range.toId() - range.fromId() + 1;
        int pages = (int) Math.ceil(total / (double) PAGE_SIZE);

        int processed = 0;

        for (int page = 0; page < pages; page++) {
            List<Product> chunk = repository.findRange(
                    range.fromId(), range.toId(),
                    PageRequest.of(page, PAGE_SIZE)
            );

            for (Product p : chunk) {
                p.changePrice(range.newPrice());
                processed++;

                if (processed % FLUSH_INTERVAL == 0) {
                    em.flush();
                    em.clear();
                }
            }
        }

        em.flush();
        em.clear();
    }
}

