package com.example.jpa_concurrency_performance_lab.service.product.strategy;

import java.util.ArrayList;
import java.util.List;

import com.example.jpa_concurrency_performance_lab.dto.UpdateRange;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcBatchStrategy implements BulkUpdateStrategy {

    private static final int BATCH_SIZE = 200;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String name() {
        return "JDBC_BATCH";
    }

    @Override
    @Transactional
    public void execute(UpdateRange range) {
        List<Long> ids = new ArrayList<>();
        for (long id = range.fromId(); id <= range.toId(); id++) {
            ids.add(id);
        }

        jdbcTemplate.batchUpdate(
                "update product set price = ? where product_id = ?",
                ids,
                BATCH_SIZE,
                (ps, id) -> {
                    ps.setInt(1, range.newPrice());
                    ps.setLong(2, id);
                }
        );
    }

    @Transactional
    public void batchUpdatePriceByIds(List<Long> ids, int newPrice, int batchSize) {
        final String sql = "UPDATE product SET price = ? WHERE product_id = ?";

        for (int start = 0; start < ids.size(); start += batchSize) {
            int end = Math.min(start + batchSize, ids.size());
            List<Long> chunk = ids.subList(start, end);

            jdbcTemplate.batchUpdate(
                    sql,
                    chunk,
                    chunk.size(),
                    (ps, id) -> {
                        ps.setInt(1, newPrice);
                        ps.setLong(2, id);
                    }
            );
        }
    }
}
