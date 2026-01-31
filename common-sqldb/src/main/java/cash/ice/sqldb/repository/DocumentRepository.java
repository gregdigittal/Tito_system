package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @Query(nativeQuery = true, value = "select * from document where entity_id is null and meta_data->'$.addressId' is null",
            countQuery = "select count(distinct id) from icecash.document where entity_id is null and meta_data->'$.addressId' is null")
    Page<Document> findUnassigned(Pageable pageable);

    @Query(nativeQuery = true, value = "select * from document where (:entityId is null or entity_id = :entityId) and (:addressId is null or meta_data->'$.addressId' = :addressId)",
            countQuery = "select count(distinct id) from icecash.document where (:entityId is null or entity_id = :entityId) and (:addressId is null or meta_data->'$.addressId' = :addressId)")
    Page<Document> findDocuments(@Param("entityId") Integer entityId, @Param("addressId") Integer addressId, Pageable pageable);

    @Query(nativeQuery = true, value = "select * from document where meta_data->'$.journalId' in :journalIds",
            countQuery = "select count(distinct id) from icecash.document where meta_data->'$.journalId' in :journalIds")
    Page<Document> findJournalsDocuments(@Param("journalIds") List<Integer> journalIds, Pageable pageable);

    Optional<Document> findByEntityIdAndDocumentTypeId(Integer entityId, Integer documentTypeId);

    List<Document> findByPath(String path);

    List<Document> findByEntityId(Integer entityId);
}
