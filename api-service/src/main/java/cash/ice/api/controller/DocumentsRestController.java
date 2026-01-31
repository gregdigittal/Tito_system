package cash.ice.api.controller;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.dto.DocumentUploadResponse;
import cash.ice.api.dto.DocumentView;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.api.service.DocumentsService;
import cash.ice.api.service.JournalService;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Document;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@RestController
@RequestMapping(value = {"/api/v1/documents", "/api/v1/unsecure/documents"})
@RequiredArgsConstructor
@Validated
@Slf4j
public class DocumentsRestController {
    private final DocumentsService documentsService;
    private final JournalService journalService;

    @GetMapping
    public Page<DocumentView> getDocuments(@RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "size", defaultValue = "10") int size,
                                           @RequestParam(value = "unassigned", required = false) boolean unassigned,
                                           @RequestParam(value = "entityId", required = false) Integer entityId,
                                           @RequestParam(value = "addressId", required = false) Integer addressId) {
        log.debug("> GET documents: page={}, size={}, unassigned: {} entityId: {}, addressId: {}",
                page, size, unassigned, entityId, addressId);
        Page<Document> documents = documentsService.getDocuments(unassigned, entityId, addressId,
                PageRequest.of(page, size));
        return documents.map(DocumentView::create);
    }

    @GetMapping("/{id}/download")
    public void getDocumentContent(HttpServletResponse response,
                                   @PathVariable(value = "id") Integer id) throws Exception {
        log.debug("> GET document content, id: {}", id);
        DocumentContent documentContent = documentsService.getDocumentContent(id);
        response.setContentType(documentContent.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + documentContent.getFileName());
        response.setContentLength(documentContent.getContentLength());
        IOUtils.copy(new ByteArrayInputStream(documentContent.getContent()), response.getOutputStream());
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public void uploadDocuments(@RequestPart(value = "files") MultipartFile[] files,
                                @RequestPart(value = "entityId", required = false) @Valid @Positive String entityId,
                                @RequestPart(value = "addressId", required = false) @Valid @Positive String addressId,
                                @RequestPart(value = "typeId", required = false) @Valid @Positive String typeId,
                                @RequestPart(value = "comments", required = false) String comments) {

        log.debug("> Upload documents: files count: {}, entityId: {}, addressId: {}, typeId: {}, comments: {}",
                files.length, entityId, addressId, typeId, comments);
        Arrays.stream(files).forEach(file -> {
            try {
                documentsService.uploadDocument(file.getBytes(), new Document()
                        .setFileName(file.getOriginalFilename())
                        .setDocumentTypeId(typeId == null ? null : Integer.valueOf(typeId))
                        .setEntityId(entityId == null ? null : Integer.valueOf(entityId))
                        .placeAddressId(addressId == null ? null : Integer.valueOf(addressId))
                        .setComments(comments)
                        .setCreatedDate(Tool.currentDateTime()));
            } catch (IOException e) {
                throw new DocumentUploadingException(e.getMessage(), e);
            }
        });
    }

    @PostMapping(value = "/journal/upload")
    @ResponseStatus(code = HttpStatus.CREATED)
    public DocumentUploadResponse uploadJournalDocument(@RequestPart(value = "file") MultipartFile file,
                                                        @RequestPart(value = "journalId") @Valid @Positive String journalId,
                                                        @RequestPart(value = "removeJournalOnFail", required = false) @Valid @Pattern(regexp = "^(true|false)$",
                                                                message = "must be boolean value (true or false)") String removeJournalOnFail) {
        int jId = Integer.parseInt(journalId);
        journalService.getJournal(jId);
        try {
            log.debug("> Upload journal document: journalId: {}, fileName: {}, removeJournalOnFail: {}",
                    jId, file.getOriginalFilename(), removeJournalOnFail);
            Document document = documentsService.uploadJournalDocument(file.getBytes(), new Document()
                    .setFileName(file.getOriginalFilename())
                    .placeJournalId(jId)
                    .setCreatedDate(Tool.currentDateTime()));
            return new DocumentUploadResponse(document.getId());
        } catch (Exception e) {
            if (Objects.equals(removeJournalOnFail, "true")) {
                documentsService.failoverDeleteJournalDocuments(jId);
                journalService.failoverDeleteJournal(jId);
            }
            throw new DocumentUploadingException(e.getMessage(), e);
        }
    }

    @PutMapping
    public void updateDocument(@Valid @RequestBody DocumentView documentView) {
        log.debug("> Update document: {}", documentView);
        documentsService.updateDocument(documentView);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Integer id) throws Exception {
        log.debug("> DELETE document, id: {}", id);
        documentsService.deleteDocument(id);
    }
}
