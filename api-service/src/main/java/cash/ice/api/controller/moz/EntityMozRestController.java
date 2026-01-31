package cash.ice.api.controller.moz;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.DocumentUploadResponse;
import cash.ice.api.dto.FileContent;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.api.dto.moz.RegisterEntityMozRequest;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.api.service.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.EntityClass;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping(value = {"/api/v1/moz/user"})
@RequiredArgsConstructor
@Validated
@Slf4j
public class EntityMozRestController {
    private final EntityRegistrationMozService entityRegistrationMozService;
    private final EntityMozService entityMozService;
    private final FileMozService fileMozService;
    private final DocumentsService documentsService;
    private final AuthUserService authUserService;

    @PostMapping(value = "/register", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(code = HttpStatus.CREATED)
    public RegisterResponse createEntity(@RequestParam(value = "firstName") @Valid @NotBlank String firstName,
                                         @RequestParam(value = "lastName") @Valid @NotBlank String lastName,
                                         @RequestParam(value = "email", required = false) @Valid @Email String email,
                                         @RequestParam(value = "mobile") @Valid @NotBlank String mobile,
                                         @RequestParam(value = "idNumber") @Valid @NotBlank String idNumber,
                                         @RequestParam(value = "pin") @Valid @NotBlank String pin,
                                         @RequestParam(value = "locale") Locale locale,
                                         @RequestPart(value = "photo", required = false) MultipartFile photoFile) {
        try {
            log.info("> Register entity (Moz): {} {}, email: {}, mobile: {}, id: {}, pin: {}, locale: {}, photo: {}",
                    firstName, lastName, email, mobile, idNumber, pin != null ? "*".repeat(pin.length()) : null, locale, photoFile != null ? photoFile.getBytes().length : 0);
            return entityRegistrationMozService.registerEntity(new RegisterEntityMozRequest()
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setEmail(email)
                    .setMobile(mobile)
                    .setIdNumber(idNumber)
                    .setPin(pin)
                    .setLocale(locale)
                    .setPhoto(photoFile != null ? photoFile.getBytes() : null)
                    .setPhotoFileName(photoFile != null ? photoFile.getOriginalFilename() : null));
        } catch (IOException e) {
            throw new MozRegistrationException(ErrorCodes.EC1015, "Uploading photo to minio failed: " + e.getMessage(), e);
        }
    }

    @PostMapping(value = "/documents/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(code = HttpStatus.CREATED)
    public DocumentUploadResponse uploadDocument(@RequestPart(value = "file") MultipartFile file,
                                                 @RequestParam(value = "documentTypeId") Integer documentTypeId) {
        try {
            log.debug("> Upload document: documentTypeId: {}, fileName: {}", documentTypeId, file.getOriginalFilename());
            Document document = documentsService.uploadDocument(file.getBytes(), new Document()
                    .setFileName(file.getOriginalFilename())
                    .setDocumentTypeId(documentTypeId)
                    .setCreatedDate(Tool.currentDateTime()));
            return new DocumentUploadResponse(document.getId());
        } catch (IOException e) {
            throw new DocumentUploadingException(e.getMessage(), e);
        }
    }

    @PostMapping(value = "/kyc/documents/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public DocumentUploadResponse uploadKycDocument(@RequestPart(value = "file") MultipartFile file,
                                                    @RequestParam(value = "entityId") Integer entityId,
                                                    @RequestParam(value = "documentTypeId") Integer documentTypeId,
                                                    @RequestParam(value = "idType", required = false) IdTypeMoz idType,
                                                    @RequestParam(value = "idNumber", required = false) String idNumber) {
        try {
            log.debug("> Upload documents: entityId: {}, documentTypeId: {}, fileName: {}, idType: {}, idNumber: {}",
                    entityId, documentTypeId, file.getOriginalFilename(), idType, idNumber);
            Document document = fileMozService.uploadKycDocument(idType, idNumber, file.getBytes(), new Document()
                    .setFileName(file.getOriginalFilename())
                    .setEntityId(entityId)
                    .setDocumentTypeId(documentTypeId)
                    .setCreatedDate(Tool.currentDateTime()));
            return new DocumentUploadResponse(document.getId());
        } catch (IOException e) {
            throw new DocumentUploadingException(e.getMessage(), e);
        }
    }

    @GetMapping("/photo/download")
    @PreAuthorize("isAuthenticated()")
    public void downloadPhoto(HttpServletResponse response) throws Exception {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        log.debug("> GET photo content for: {}) {} {}, {}", authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName(), authEntity.getEmail());

        FileContent fileContent = fileMozService.getPhoto(authEntity);
        response.setContentType(fileContent.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileContent.getFileName());
        response.setContentLength(fileContent.getContentLength());
        IOUtils.copy(new ByteArrayInputStream(fileContent.getContent()), response.getOutputStream());
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}
