package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Integer> {

    Optional<EmailTemplate> findByTypeAndLanguageId(String type, Integer languageId);
}
