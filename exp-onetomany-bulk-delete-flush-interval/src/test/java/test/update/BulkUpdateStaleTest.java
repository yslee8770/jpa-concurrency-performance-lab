package test.update;

import com.example.UpdateLabApplication;
import com.example.domain.Product;
import com.example.domain.ProductStatus;
import com.example.repository.ProductRepository;
import com.example.service.ProductBulkUpdateService;
import com.example.setup.UpdateDataSeeder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UpdateLabApplication.class)
class BulkUpdateStaleTest {

    @Autowired
    UpdateDataSeeder seeder;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductBulkUpdateService bulkService;
    @Autowired EntityManager em;

    @BeforeEach
    void setUp() {
        seeder.reseedProducts(1000, 1000, 1000);
    }

    @Test
    void bulk_update_causes_persistence_context_stale() {
        // given
        Product product = productRepository.findById(1L).orElseThrow();
        assertThat(product.getPrice()).isEqualTo(1000);

        // when
        bulkService.bulkUpdatePrice(ProductStatus.ON, 2000);

        // then
        // ️⃣ 영속성 컨텍스트에 있는 엔티티는 stale
        assertThat(product.getPrice())
                .as("영속성 컨텍스트의 값은 bulk update 후에도 변경되지 않는다")
                .isEqualTo(1000);

        // 2⃣ DB에서는 값이 바뀌어 있음 (clear 후 확인)
        em.clear();
        Product refreshed = productRepository.findById(1L).orElseThrow();

        assertThat(refreshed.getPrice()).isEqualTo(2000);
    }

    @Test
    void bulk_update_with_clear_keeps_consistency() {
        // given
        Product product = productRepository.findById(1L).orElseThrow();

        // when
        bulkService.bulkUpdatePriceAndClear(ProductStatus.ON, 3000);

        // then
        Product refreshed = productRepository.findById(1L).orElseThrow();
        assertThat(refreshed.getPrice()).isEqualTo(3000);
    }

    @Test
    void bulk_update_stale_and_clear_solution() {
        Product product = productRepository.findById(1L).orElseThrow();

        bulkService.bulkUpdatePrice(ProductStatus.ON, 2000);

        // stale
        assertThat(product.getPrice()).isEqualTo(1000);

        em.clear();
        Product refreshed = productRepository.findById(1L).orElseThrow();

        // resolved
        assertThat(refreshed.getPrice()).isEqualTo(2000);
    }
}

