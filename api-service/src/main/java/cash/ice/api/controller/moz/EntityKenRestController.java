package cash.ice.api.controller.moz;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.dto.DocumentUploadResponse;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.api.service.DocumentsService;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Document;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping(value = {"/api/v1/ken/user"})
@RequiredArgsConstructor
@Validated
@Slf4j
public class EntityKenRestController {
    private final DocumentsService documentsService;

    @PostMapping(value = "/documents/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(code = HttpStatus.CREATED)
    public DocumentUploadResponse uploadDocument(@RequestPart(value = "file") MultipartFile file,
                                                 @RequestParam(value = "documentType") String documentType) {
        try {
            log.debug("> Upload document: documentType: '{}', fileName: '{}'", documentType, file.getOriginalFilename());
            Document document = documentsService.uploadKenDocument(file.getBytes(), documentType, new Document()
                    .setFileName(file.getOriginalFilename())
                    .setCreatedDate(Tool.currentDateTime()));
            log.debug("  uploaded document id={}, type: '{}', fileName: '{}'", document.getId(), documentType, file.getOriginalFilename());
            return new DocumentUploadResponse(document.getId());
        } catch (IOException e) {
            throw new DocumentUploadingException(e.getMessage(), e);
        }
    }

    @GetMapping("/{entityId}/documents/{documentType}/download")
    public void getDocumentContent(HttpServletResponse response,
                                   @PathVariable(value = "entityId") Integer entityId,
                                   @PathVariable(value = "documentType") String documentType) throws Exception {
        log.debug("> GET document content, entityId: '{}', documentType: '{}'", entityId, documentType);
        DocumentContent documentContent = documentsService.getKenDocumentContent(entityId, documentType);
        response.setContentType(documentContent.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + documentContent.getFileName());
        response.setContentLength(documentContent.getContentLength());
        log.debug("  document '{}' (id={}) content size: {}", documentContent.getFileName(), documentContent.getDocument().getId(), documentContent.getContentLength());
        IOUtils.copy(new ByteArrayInputStream(documentContent.getContent()), response.getOutputStream());
    }
}
