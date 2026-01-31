package cash.ice.sync.service;

import cash.ice.sqldb.repository.DocumentTypeRepository;
import cash.ice.sync.dto.DocumentTypeChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class DocumentTypesSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.Documents_Types";
    private static final String DOCUMENT_TYPE = "DocumentType";
    private static final String REQUIRED = "Required";

    private final JdbcTemplate jdbcTemplate;
    private final DocumentTypeRepository documentTypeRepository;

    @Transactional
    @Override
    public void migrateData() {
//        log.debug("Start migrating Document Types");
//        List<DocumentType> documentTypes = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
//                new DocumentType()
//                        .setDocumentType(rs.getString("Document_Type"))
//                        .setAccountTypeId(rs.getInt("Account_Type_ID"))
//                        .setRequired(rs.getBoolean(REQUIRED))
//                        .setDocumentTypeId(rs.getInt("Document_Type_ID")));
//        documentTypeRepository.saveAll(documentTypes);
//        log.info("Finished migrating Document Types: {} processed, {} total", documentTypes.size(), documentTypeRepository.count());
    }

    public void update(DocumentTypeChange dataChange) {
//        DocumentType documentType = documentTypeRepository.findByDocumentTypeIdAndAccountTypeId(
//                dataChange.getDocumentTypeId(), dataChange.getAccountTypeId()).orElse(null);
//        if (dataChange.getAction() == ChangeAction.DELETE) {
//            if (documentType != null) {
//                documentTypeRepository.delete(documentType);
//            } else {
//                log.warn("Cannot delete Document Type with id: {} and Account Type id: {}, it is absent",
//                        dataChange.getDocumentTypeId(), dataChange.getAccountTypeId());
//            }
//        } else {                // update
//            if (documentType == null) {
//                documentType = new DocumentType()
//                        .setDocumentTypeId(dataChange.getDocumentTypeId())
//                        .setAccountTypeId(dataChange.getAccountTypeId());
//            }
//            if (dataChange.getData().containsKey(DOCUMENT_TYPE)) {
//                documentType.setDocumentType((String) dataChange.getData().get(DOCUMENT_TYPE));
//            }
//            if (dataChange.getData().containsKey(REQUIRED)) {
//                documentType.setRequired((Boolean) dataChange.getData().get(REQUIRED));
//            }
//            documentTypeRepository.save(documentType);
//        }
    }
}
