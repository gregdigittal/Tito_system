package cash.ice.api.repository.backoffice;

import cash.ice.api.entity.backoffice.Journal;
import cash.ice.api.entity.backoffice.JournalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface JournalRepository extends JpaRepository<Journal, Integer> {

    Page<Journal> findByStatusAndCreatedDateAfter(JournalStatus journalStatus, LocalDateTime createdDate, Pageable pageable);
}