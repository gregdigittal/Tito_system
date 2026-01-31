package cash.ice.api.controller.backoffice;

import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.JournalFee;
import cash.ice.api.entity.backoffice.Journal;
import cash.ice.api.entity.backoffice.JournalStatus;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.JournalService;
import cash.ice.api.service.StaffMemberService;
import cash.ice.api.util.MappingUtil;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.TransactionCode;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.TransactionCodeRepository;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class JournalController {
    private final JournalService journalService;
    private final AuthUserService authUserService;
    private final StaffMemberService staffMemberService;
    private final TransactionCodeRepository transactionCodeRepository;
    private final AccountRepository accountRepository;

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_BACKOFFICE')")
    public Journal createJournal(@Argument Journal journal, @Argument List<JournalFee> fees, @Argument String url, @Argument boolean sendEmail) {
        StaffMember staffMember = staffMemberService.getAuthStaffMember(authUserService.getAuthUser(), null);
        log.debug("> create journal: {}, fees: {}, url: {}, sendEmail: {}, staffMember: (id={}, {})", journal, fees, url, sendEmail, staffMember != null ? staffMember.getId() : null, staffMember != null ? staffMember.getEmail() : null);
        return journalService.createJournal(journal, fees, url, sendEmail, staffMember);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_BACKOFFICE')")
    public Journal rejectJournal(@Argument Integer journalId) {
        StaffMember staffMember = staffMemberService.getAuthStaffMember(authUserService.getAuthUser(), null);
        log.debug("> reject journal: {}, staffMember: (id={}, {})", journalId, staffMember != null ? staffMember.getId() : null, staffMember != null ? staffMember.getEmail() : null);
        return journalService.rejectJournal(journalId, staffMember);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_BACKOFFICE')")
    public Journal acceptJournal(@Argument Integer journalId) {
        StaffMember staffMember = staffMemberService.getAuthStaffMember(authUserService.getAuthUser(), null);
        log.debug("> accept journal: {}, staffMember: (id={}, {})", journalId, staffMember != null ? staffMember.getId() : null, staffMember != null ? staffMember.getEmail() : null);
        return journalService.acceptJournal(journalId, staffMember);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ROLE_BACKOFFICE')")
    public Page<Journal> journals(@Argument JournalStatus journalStatus, @Argument int days, @Argument int page, @Argument int size, @Argument SortInput sort) {
        StaffMember staffMember = staffMemberService.getAuthStaffMember(authUserService.getAuthUser(), null);
        log.info("> GET {} journals for {} days staffMember: (id={}, {}), page: {}, size: {}, sort: {}",
                journalStatus, days, staffMember != null ? staffMember.getId() : null, staffMember != null ? staffMember.getEmail() : null, page, size, sort);
        if (staffMember == null) {
            throw new NotAuthorizedException("Operation is allowed only for backoffice user");
        }
        return journalService.getJournals(journalStatus, days, page, size, sort);
    }

    @BatchMapping(typeName = "Journal", field = "transactionCode")
    public Map<Journal, TransactionCode> journalTransactionCode(List<Journal> journals) {
        return itemsToCategoriesMap(journals, Journal::getTransactionCodeId, TransactionCode::getId, transactionCodeRepository);
    }

    @BatchMapping(typeName = "Journal", field = "drAccount")
    public Map<Journal, Account> journalDrAccount(List<Journal> journals) {
        return MappingUtil.itemsToCategoriesMap(journals, Journal::getDrAccountId, Account::getId, accountRepository);
    }

    @BatchMapping(typeName = "Journal", field = "crAccount")
    public Map<Journal, Account> journalCrAccount(List<Journal> journals) {
        return MappingUtil.itemsToCategoriesMap(journals, Journal::getCrAccountId, Account::getId, accountRepository);
    }

    @BatchMapping(typeName = "Journal", field = "fees")
    public Map<Journal, List<Map<String, Object>>> journalFees(List<Journal> journals) {
        return journalService.toMapJournalFees(journals);
    }

    @BatchMapping(typeName = "Journal", field = "documents")
    public Map<Journal, List<Document>> journalDocuments(List<Journal> journals) {
        return journalService.toMapJournalDocuments(journals);
    }
}
