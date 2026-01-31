package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Integer> {

    Optional<Language> findByLanguageKey(String languageKey);
}
