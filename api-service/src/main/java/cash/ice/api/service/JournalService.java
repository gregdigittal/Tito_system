package cash.ice.api.service;

import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.JournalFee;
import cash.ice.api.entity.backoffice.Journal;
import cash.ice.api.entity.backoffice.JournalStatus;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.sqldb.entity.Document;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface JournalService {

    Journal createJournal(Journal journal, List<JournalFee> fees, String url, boolean sendEmail, StaffMember creatorStaffMember);

    Journal deleteJournal(Integer journalId);

    Journal rejectJournal(Integer journalId, StaffMember staffMember);

    Journal acceptJournal(Integer journalId, StaffMember staffMember);

    Map<Journal, List<Map<String, Object>>> toMapJournalFees(List<Journal> journals);

    Journal getJournal(Integer journalId);

    Page<Journal> getJournals(JournalStatus status, int days, int page, int size, SortInput sort);

    Map<Journal, List<Document>> toMapJournalDocuments(List<Journal> journals);

    void failoverDeleteJournal(Integer journalId);
}
