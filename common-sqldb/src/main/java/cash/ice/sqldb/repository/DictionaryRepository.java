package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Dictionary;
import cash.ice.sqldb.entity.DictionaryEnvSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DictionaryRepository extends JpaRepository<Dictionary, Integer> {

    List<Dictionary> findByLanguageIdAndSectionInAndEnvironment(int languageId, List<String> sections, String environment);

    List<Dictionary> findByLanguageIdAndEnvironment(int languageId, String environment);

    Optional<Dictionary> findByLanguageIdAndLookupKey(int languageId, String lookupKey);

    @Query("select distinct new cash.ice.sqldb.entity.DictionaryEnvSection(d.languageId, d.environment, d.section) " +
            "from Dictionary d where d.languageId in :languageIds")
    List<DictionaryEnvSection> getEnvironmentSections(@Param("languageIds") List<Integer> languageIds);
}
