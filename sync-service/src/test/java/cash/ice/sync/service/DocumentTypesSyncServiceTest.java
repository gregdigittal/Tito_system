package cash.ice.sync.service;

import cash.ice.sqldb.repository.DocumentTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.URISyntaxException;

@ExtendWith(MockitoExtension.class)
class DocumentTypesSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private DocumentTypesSyncService service;

    @BeforeEach
    void init() {
        service = new DocumentTypesSyncService(jdbcTemplate, documentTypeRepository);
    }

    @Test
    void testDocumentTypeCreate() throws URISyntaxException, IOException {
//        DocumentTypeChange dataChange = objectMapper.readValue(
//                Tool.readResourceAsString("update/json/documentTypeRequest.json"), DocumentTypeChange.class);
//        service.update(dataChange);
//        verify(documentTypeRepository).save(new DocumentType().setDocumentTypeId(1).setAccountTypeId(1)
//                .setRequired(true).setDocumentType("Proof of Address"));
    }

    @Test
    void testDocumentTypeUpdate() throws URISyntaxException, IOException {
//        DocumentTypeChange dataChange = objectMapper.readValue(
//                Tool.readResourceAsString("update/json/documentTypeRequest.json"), DocumentTypeChange.class);
//        when(documentTypeRepository.findByDocumentTypeIdAndAccountTypeId(dataChange.getDocumentTypeId(),
//                dataChange.getAccountTypeId())).thenReturn(Optional.of(new DocumentType()
//                .setDocumentTypeId(1).setAccountTypeId(1)));
//        service.update(dataChange);
//        verify(documentTypeRepository).save(new DocumentType()
//                .setDocumentTypeId(1).setAccountTypeId(1).setRequired(true).setDocumentType("Proof of Address"));
    }

    @Test
    void testDocumentTypeDelete() {
//        DocumentTypeChange dataChange = new DocumentTypeChange().setAction(ChangeAction.DELETE).setDocumentTypeId(1).setAccountTypeId(2);
//        DocumentType documentType = new DocumentType().setDocumentTypeId(dataChange.getDocumentTypeId())
//                .setAccountTypeId(dataChange.getAccountTypeId());
//        when(documentTypeRepository.findByDocumentTypeIdAndAccountTypeId(dataChange.getDocumentTypeId(),
//                dataChange.getAccountTypeId())).thenReturn(Optional.of(documentType));
//        service.update(dataChange);
//        verify(documentTypeRepository).delete(documentType);
    }
}