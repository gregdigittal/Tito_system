package cash.ice.api.repository.ken;

import cash.ice.api.entity.ken.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

}