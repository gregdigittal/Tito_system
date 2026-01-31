package cash.ice.api.repository.ken;

import cash.ice.api.entity.ken.EntityProduct;
import cash.ice.api.entity.moz.ProductRelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntityProductRepository extends JpaRepository<EntityProduct, Integer> {

    List<EntityProduct> findByEntityIdAndRelationshipType(Integer entityId, ProductRelationshipType relationshipType);

    List<EntityProduct> findByEntityIdAndProductIdAndRelationshipType(Integer entityId, Integer productId, ProductRelationshipType relationshipType);

    List<EntityProduct> findByProductId(Integer productId);

    List<EntityProduct> findByEntityIdIn(List<Integer> entityIds);
}