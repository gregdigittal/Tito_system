package cash.ice.api.controller;

import cash.ice.api.dto.SortInput;
import cash.ice.sqldb.entity.Country;
import cash.ice.sqldb.entity.Dictionary;
import cash.ice.sqldb.entity.DictionaryEnvSection;
import cash.ice.sqldb.entity.Language;
import cash.ice.sqldb.repository.CountryRepository;
import cash.ice.sqldb.repository.DictionaryRepository;
import cash.ice.sqldb.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class LanguageController {
    private final LanguageRepository languageRepository;
    private final DictionaryRepository dictionaryRepository;
    private final CountryRepository countryRepository;

    @MutationMapping
    public Language addLanguage(@Argument Language language) {
        return languageRepository.save(language);
    }

    @MutationMapping
    public Optional<Language> updateLanguage(@Argument Integer id, @Argument Language language) {
        return languageRepository.findById(id).map(language1 ->
                languageRepository.save(language.setId(id)));
    }

    @MutationMapping
    public Optional<Language> deleteLanguage(@Argument Integer id) {
        Optional<Language> language = languageRepository.findById(id);
        language.ifPresent(languageRepository::delete);
        return language;
    }

    @MutationMapping
    public Dictionary addDictionary(@Argument Dictionary dictionary) {
        return dictionaryRepository.save(dictionary);
    }

    @MutationMapping
    public Optional<Dictionary> updateDictionary(@Argument Integer id, @Argument Dictionary dictionary) {
        return dictionaryRepository.findById(id).map(dictionary1 ->
                dictionaryRepository.save(dictionary.setId(id)));
    }

    @MutationMapping
    public Optional<Dictionary> deleteDictionary(@Argument Integer id) {
        Optional<Dictionary> dictionary = dictionaryRepository.findById(id);
        dictionary.ifPresent(dictionaryRepository::delete);
        return dictionary;
    }

    @QueryMapping
    public Iterable<Language> allLanguages(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return languageRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    public Iterable<Dictionary> dictionary(@Argument int languageId, @Argument List<String> sections, @Argument String env) {
        if (sections.isEmpty()) {
            return dictionaryRepository.findByLanguageIdAndEnvironment(languageId, env);
        } else {
            return dictionaryRepository.findByLanguageIdAndSectionInAndEnvironment(languageId, sections, env);
        }
    }

    @QueryMapping
    public Map<String, Object> dictionaryMap(@Argument int languageId, @Argument List<String> sections, @Argument String env) {
        List<Dictionary> dictionaries;
        if (sections.isEmpty()) {
            dictionaries = dictionaryRepository.findByLanguageIdAndEnvironment(languageId, env);
        } else {
            dictionaries = dictionaryRepository.findByLanguageIdAndSectionInAndEnvironment(languageId, sections, env);
        }
        return dictionaries.stream().collect(Collectors.toMap(Dictionary::getLookupKey, Dictionary::getValue));
    }

    @BatchMapping(typeName = "Language", field = "dictionarySections")
    public Map<Language, Map<String, List<String>>> entityType(List<Language> languages) {
        List<Integer> ids = languages.stream().map(Language::getId).toList();
        Map<Integer, Language> languagesMap = languages.stream().collect(Collectors.toMap(Language::getId, v -> v));
        return dictionaryRepository.getEnvironmentSections(ids).stream().collect(
                        Collectors.groupingBy(DictionaryEnvSection::getLanguageId,
                                Collectors.groupingBy(DictionaryEnvSection::getEnvironment,
                                        Collectors.mapping(DictionaryEnvSection::getSection, Collectors.toList()))))
                .entrySet().stream().map(entry -> new AbstractMap.SimpleEntry<>(languagesMap.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @QueryMapping
    public Iterable<Country> allCountries(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return countryRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }
}
