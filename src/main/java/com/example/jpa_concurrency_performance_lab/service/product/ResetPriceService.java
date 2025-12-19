package com.example.jpa_concurrency_performance_lab.service.product;

import com.example.jpa_concurrency_performance_lab.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPriceService {
    private final ProductRepository repository;

    //트랜잭션 분리용 서비스
    @Transactional
    public int resetPrice(long from, long to, int resetPrice) {
        return repository.resetPrice(from, to, resetPrice);
    }
}
