package cash.ice.fbc.controller;

import cash.ice.fbc.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/zim/fbc/server")
@RequiredArgsConstructor
@Profile("!prod-k8s")
@Slf4j
public class FbcTestServerUatController {

    @PostMapping("/login")
    public ResponseEntity<FbcLoginResponse> fbcLogin(@Valid @RequestBody FbcLoginRequest request, @RequestHeader("User-Agent") String userAgent) {
        log.debug("|> fbc login: {}, user-agent: {}", request, userAgent);
        if (!"icecash".equals(request.getUsername()) || !"icecash123*".equals(request.getPassword())) {
            return new ResponseEntity<>(new FbcLoginResponse().setStatus("UNAUTHORIZED").setMessage("Bad credentials"), HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(new FbcLoginResponse().setToken(makeToken()), HttpStatus.OK);
    }

    private String makeToken() {
        return "token-" + (LocalDateTime.now().getMinute());
    }

    @GetMapping("/onlinepayments/v2/onus/account/details/{accountNumber}")
    public ResponseEntity<FbcVerificationResponse> fbcVerification(@PathVariable String accountNumber, @RequestHeader("User-Agent") String userAgent, @RequestHeader("Authorization") String auth) {
        log.debug("|> fbc verification: {}, user-agent: {}, auth: {}", accountNumber, userAgent, auth);
        if (!("Bearer " + makeToken()).equals(auth)) {
            log.info("|  token is expired or absent: {}, expected: {}", auth, makeToken());
            return new ResponseEntity<>(new FbcVerificationResponse()
                    .setTimestamp(Instant.now())
                    .setStatus(401)
                    .setError("Unauthorized"), HttpStatus.UNAUTHORIZED);
        } else if (!"2170031060127".equals(accountNumber)) {
            log.info("|  account is not '2170031060127'");
            return new ResponseEntity<>(new FbcVerificationResponse()
                    .setTimestamp(Instant.now())
                    .setStatus(500)
                    .setError("Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
//        Objects.requireNonNull(accountNumber, "'accountNumber' is not provided");
        return new ResponseEntity<>(new FbcVerificationResponse()
                .setStatusCode(100)
                .setResponse(new FbcVerificationResponse.Response()
                        .setCustomerAccountNumber(accountNumber)
                        .setBranchCode("020")
                        .setAccountDescription("MAKARA SHEPHARD")
                        .setCustomerNumber("7003106")
                        .setRecordStat("O")), HttpStatus.OK);
    }

    @PostMapping("/onlinepayments/v2/onus/generate/otp/")
    public ResponseEntity<FbcGenerateOtpResponse> fbcGenerateOtp(@Valid @RequestBody FbcGenerateOtpRequest request, @RequestHeader("User-Agent") String userAgent, @RequestHeader("Authorization") String auth) {
        log.debug("|> fbc generate otp: {}, user-agent: {}, auth: {}", request, userAgent, auth);
        if (!("Bearer " + makeToken()).equals(auth)) {
            log.info("|  token is expired or absent: {}, expected: {}", auth, makeToken());
            return new ResponseEntity<>(new FbcGenerateOtpResponse()
                    .setTimestamp(Instant.now())
                    .setStatus(401)
                    .setError("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(new FbcGenerateOtpResponse()
                .setStatusCode(100)
                .setResponse(new FbcGenerateOtpResponse.Response()
                        .setBranchCode("020")
                        .setPhoneNumber("000000000000")
                        .setAccountNumber(request.getAccount())), HttpStatus.OK);
    }

    @PostMapping("/onlinepayments/v2/onus/verify/otp")
    public ResponseEntity<FbcVerifyOtpResponse> fbcVerifyOtp(@Valid @RequestBody FbcVerifyOtpRequest request, @RequestHeader("User-Agent") String userAgent, @RequestHeader("Authorization") String auth) {
        log.debug("|> fbc verify otp: {}, user-agent: {}, auth: {}", request, userAgent, auth);
        if (!("Bearer " + makeToken()).equals(auth)) {
            log.info("|  token is expired or absent: {}, expected: {}", auth, makeToken());
            return new ResponseEntity<>(new FbcVerifyOtpResponse()
                    .setTimestamp(Instant.now())
                    .setStatus(401)
                    .setError("Unauthorized"), HttpStatus.UNAUTHORIZED);
        } else if (!"000000".equals(request.getOtp())) {
            log.info("|  OTP is not '000000'");
            return new ResponseEntity<>(new FbcVerifyOtpResponse()
                    .setStatusCode(106)
                    .setResponse("INVALID_OTP_OR_OTP_EXPIRED"), HttpStatus.OK);
        }
//        Objects.requireNonNull(request.getOtp(), "'otp' field is not provided");
        Objects.requireNonNull(request.getTransactionReference(), "'transactionReference' field is not provided");
        return new ResponseEntity<>(new FbcVerifyOtpResponse()
                .setStatusCode(100)
                .setResponse("SUCCESS"), HttpStatus.OK);
    }

    @PostMapping("/onlinepayments/v2/onus/submit/onus/transfer")
    public ResponseEntity<FbcTransferSubmissionResponse> fbcTransferSubmission(@Valid @RequestBody FbcTransferSubmissionRequest request, @RequestHeader("User-Agent") String userAgent, @RequestHeader("Authorization") String auth) {
        log.debug("|> fbc transfer submission: {}, user-agent: {}, auth: {}", request, userAgent, auth);
        if (!("Bearer " + makeToken()).equals(auth)) {
            log.info("|  token is expired or absent: {}, expected: {}", auth, makeToken());
            return new ResponseEntity<>(new FbcTransferSubmissionResponse()
                    .setTimestamp(Instant.now())
                    .setStatus(401)
                    .setError("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
        Objects.requireNonNull(request.getCurrency(), "'currency' field is not provided");
        Objects.requireNonNull(request.getAmount(), "'amount' field is not provided");
        Objects.requireNonNull(request.getExternalReference(), "'externalReference' field is not provided");
        Objects.requireNonNull(request.getInitiatorID(), "'initiatorID' field is not provided");
        Objects.requireNonNull(request.getPaymentDetails(), "'paymentDetails' field is not provided");
        Objects.requireNonNull(request.getSourceAccountNumber(), "'sourceAccountNumber' field is not provided");
        Objects.requireNonNull(request.getSourceBranchCode(), "'sourceBranchCode' field is not provided");
        return new ResponseEntity<>(new FbcTransferSubmissionResponse()
                .setStatusCode(100)
                .setResponse(new FbcTransferSubmissionResponse.Response()
//                        .setResultCode("01")
//                        .setResultDescription("Failed")
//                        .setHostReference("")
//                        .setExternalReference("XX41")
                        .setBranchCode(request.getSourceBranchCode())
                        .setPhoneNumber("******009")
                        .setAccountNumber(request.getSourceAccountNumber())), HttpStatus.OK);
    }

    @GetMapping("/onlinepayments/v2/onus/query/status/{externalReference}")
    public ResponseEntity<FbcStatusResponse> fbcGetStatus(@PathVariable(name = "externalReference") String externalReference, @RequestHeader("User-Agent") String userAgent, @RequestHeader("Authorization") String auth) {
        log.debug("|> fbc get status: {}, user-agent: {}, auth: {}", externalReference, userAgent, auth);
        if (!("Bearer " + makeToken()).equals(auth)) {
            log.info("|  token is expired or absent: {}, expected: {}", auth, makeToken());
            return new ResponseEntity<>(new FbcStatusResponse()
                    .setTimestamp(Instant.now())
                    .setStatus(401)
                    .setError("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(new FbcStatusResponse()
                .setStatusCode(100)
                .setResponse(new FbcStatusResponse.Response()
                        .setBeneficiaryOrg("ICECASH")
                        .setClientId("2170259590158")
                        .setClientName("MAKARA SHEPHARD")
                        .setCreditAccount("1120153300111")
                        .setCreditBranch("005")
                        .setDebitAccount("2170259590158")
                        .setDebitAmount(new BigDecimal("0.1"))
                        .setDebitBranch("015")
                        .setDebitCurrency("ZWL")
                        .setDebitValueDate(Instant.now().toString())
                        .setExternalReference(externalReference)
                        .setHostReference("FEES")
                        .setResultCode("01")
                        .setResultDescription("Failed")), HttpStatus.OK);
    }
}
