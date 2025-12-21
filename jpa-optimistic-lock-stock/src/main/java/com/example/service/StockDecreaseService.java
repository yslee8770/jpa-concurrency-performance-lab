package com.example.service;

import com.example.domain.DecreaseResult;
import com.example.service.retry.RetryPolicy;
import com.example.domain.Stock;
import com.example.domain.exception.OutOfStockException;
import com.example.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDecreaseService {

    private final StockRepository stockRepository;
    private final PlatformTransactionManager txManager;

    public DecreaseResult decreaseWithRetry(long stockId, long amount, RetryPolicy policy) {
        long attempts = 0;
        long optimisticConflicts = 0;

        while (true) {
            attempts++;

            try {
                runInNewTx(attempts, () -> decreaseOnce(stockId, amount));
                return DecreaseResult.success(attempts, optimisticConflicts);

            } catch (OptimisticLockingFailureException e) {
                optimisticConflicts++;

                if (attempts >= policy.maxAttempts()) {
                    return DecreaseResult.optimisticMaxAttempts(attempts, optimisticConflicts);
                }
                sleep(policy.delayMs(attempts));

            } catch (OutOfStockException e) {
                return DecreaseResult.outOfStock(attempts, optimisticConflicts);
            }
        }
    }

    public void decreaseOnceWithoutRetry(long stockId, long amount) {
        runInNewTx(1, () -> decreaseOnce(stockId, amount));
    }

    private void decreaseOnce(long stockId, long amount) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("stock not found. id=" + stockId));
        stock.decrease(amount);
    }

    private void runInNewTx(long attempt, Runnable action) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        tt.execute(status -> {
            log.info("[attempt={}] isNewTx={}, txActive={}", attempt, status.isNewTransaction(), status.hasTransaction());
            action.run();
            return null;
        });
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
}
