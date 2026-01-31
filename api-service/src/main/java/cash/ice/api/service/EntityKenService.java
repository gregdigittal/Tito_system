package cash.ice.api.service;

import cash.ice.api.dto.moz.AccountTypeKen;
import cash.ice.api.dto.moz.IdTypeKen;
import cash.ice.api.entity.ken.EntityProduct;
import cash.ice.api.entity.ken.Product;
import cash.ice.api.entity.moz.ProductRelationshipType;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.sqldb.entity.EntityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface EntityKenService {

    Page<EntityClass> entitiesSearchFNDS(AccountTypeKen accountType, IdTypeKen idType, String idNumber, String mobile, PageRequest pageable);

    List<EntityProduct> getDeviceProductsFNDS(String deviceSerialOrCode);

    PaymentResponse makePayment(PaymentRequest paymentRequest);

    PaymentResponse makeBulkPayment(List<PaymentRequest> paymentRequestList);

    Product saveProduct(Product product);

    EntityProduct updateEntityProductRelationship(Integer entityId, Integer productId, ProductRelationshipType relationshipType, boolean active);

    String deleteEntityProductRelationship(Integer entityId, Integer productId, ProductRelationshipType relationshipType);

    Product removeProduct(Integer productId);
}
