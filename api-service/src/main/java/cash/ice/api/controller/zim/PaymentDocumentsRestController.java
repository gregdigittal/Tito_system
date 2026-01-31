package cash.ice.api.controller.zim;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentDocumentContent;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PaymentDocumentsService;
import cash.ice.common.constant.IceCashProfile;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@RestController
@RequestMapping(value = {"/api/v1/payments/pending"})
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Slf4j
public class PaymentDocumentsRestController {
    private final AuthUserService authUserService;
    private final PaymentDocumentsService paymentDocumentsService;

    @PostMapping(value = "/{paymentId}/documents", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(code = HttpStatus.CREATED)
    public void uploadPaymentDocuments(@RequestPart(value = "files") MultipartFile[] files,
                                       @PathVariable(value = "paymentId") Integer paymentId) {
        log.debug("> Upload payment documents: files count: {}", files.length);
        AuthUser authUser = getAuthUser();
        Arrays.stream(files).forEach(file -> {
            try {
                paymentDocumentsService.uploadDocument(file.getOriginalFilename(), file.getBytes(), paymentId, authUser);
            } catch (IOException e) {
                throw new DocumentUploadingException(e.getMessage(), e);
            }
        });
    }

    @GetMapping("/{paymentId}/documents/{documentId}")
    public void getDocumentContent(HttpServletResponse response,
                                   @PathVariable(value = "paymentId") Integer paymentId,
                                   @PathVariable(value = "documentId") String documentId) throws Exception {
        log.debug("> GET payment document content: paymentId: {}, documentId: {}", paymentId, documentId);
        AuthUser authUser = getAuthUser();
        PaymentDocumentContent documentContent = paymentDocumentsService.getDocumentContent(paymentId, documentId, authUser);
        response.setContentType(documentContent.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + documentContent.getFilename());
        response.setContentLength(documentContent.getContentLength());
        IOUtils.copy(new ByteArrayInputStream(documentContent.getContent()), response.getOutputStream());
    }

    @DeleteMapping("/{paymentId}/documents/{documentId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Integer paymentId, @PathVariable String documentId) throws Exception {
        log.debug("> DELETE payment document, paymentId: {}, documentId: {}", paymentId, documentId);
        AuthUser authUser = getAuthUser();
        paymentDocumentsService.deleteDocument(paymentId, documentId, authUser);
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}
