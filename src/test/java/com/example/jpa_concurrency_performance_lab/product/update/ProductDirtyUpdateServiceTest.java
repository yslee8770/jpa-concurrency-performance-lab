package com.example.jpa_concurrency_performance_lab.product.update;

import com.example.jpa_concurrency_performance_lab.domain.product.Product;
import com.example.jpa_concurrency_performance_lab.domain.product.ProductStatus;
import com.example.jpa_concurrency_performance_lab.repository.ProductRepository;
import com.example.jpa_concurrency_performance_lab.service.product.ProductBulkUpdateService;
import com.example.jpa_concurrency_performance_lab.service.product.ProductDirtyUpdateService;
import com.example.jpa_concurrency_performance_lab.setup.UpdateDataSeeder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnableJpaAuditing
class BulkUpdateAuditingTest {

    @Autowired
    UpdateDataSeeder seeder;
    @Autowired ProductRepository productRepository;
    @Autowired ProductBulkUpdateService bulkService;
    @Autowired ProductDirtyUpdateService dirtyService;
    @Autowired EntityManager em;

    @BeforeEach
    void setUp() {
        seeder.reseedProducts(1000, 1000, 1000);
    }

    @Test
    void dirty_checking_triggers_auditing() {
        // given
        Product product = productRepository.findById(1L).orElseThrow();
        LocalDateTime before = product.getUpdatedAt();

        // when
        dirtyService.dirtyUpdatePrice(ProductStatus.ON, 2000);

        // then
        Product refreshed = productRepository.findById(1L).orElseThrow();
        assertThat(refreshed.getUpdatedAt())
                .as("Dirty Checking은 @LastModifiedDate를 갱신한다")
                .isAfter(before);
    }

    @Test
    void bulk_update_does_not_trigger_auditing() {
        // given
        Product product = productRepository.findById(1L).orElseThrow();
        LocalDateTime before = product.getUpdatedAt();

        // when
        bulkService.bulkUpdatePrice(ProductStatus.ON, 3000);
        em.clear();

        // then
        Product refreshed = productRepository.findById(1L).orElseThrow();
        assertThat(refreshed.getUpdatedAt())
                .as("JPQL Bulk Update는 auditing을 타지 않는다")
                .isEqualTo(before);
    }
}