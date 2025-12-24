package test.update;

import com.example.UpdateLabApplication;
import com.example.domain.Product;
import com.example.domain.ProductStatus;
import com.example.repository.ProductRepository;
import com.example.service.ProductBulkUpdateService;
import com.example.service.ProductDirtyUpdateService;
import com.example.setup.UpdateDataSeeder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UpdateLabApplication.class)
class BulkUpdateAuditingTest {

    @Autowired
    UpdateDataSeeder seeder;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductBulkUpdateService bulkService;
    @Autowired
    ProductDirtyUpdateService dirtyService;
    @Autowired
    EntityManager em;

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