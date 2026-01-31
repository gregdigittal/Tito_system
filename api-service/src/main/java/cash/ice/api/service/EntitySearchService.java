package cash.ice.api.service;

import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.EntitiesSearchCriteria;
import cash.ice.sqldb.entity.EntityClass;
import org.springframework.data.domain.Page;

public interface EntitySearchService {

    Page<EntityClass> searchEntities(EntitiesSearchCriteria searchBy, String searchInput, boolean exactMatch, int page, int size, SortInput sort);
}
