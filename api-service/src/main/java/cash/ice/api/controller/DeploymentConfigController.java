package cash.ice.api.controller;

import cash.ice.api.config.property.DeploymentConfigProperties;
import cash.ice.api.dto.DeploymentConfigDto;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.Language;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 6: Public deployment config endpoint. No auth required so the app can load
 * country, terminology, currencies, and locale before login.
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Slf4j
public class DeploymentConfigController {

    private final DeploymentConfigProperties deploymentConfig;
    private final CurrencyRepository currencyRepository;
    private final LanguageRepository languageRepository;

    @GetMapping(value = "/deployment", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeploymentConfigDto getDeploymentConfig() {
        log.debug("> GET deployment config");
        List<DeploymentConfigDto.CurrencyItem> currencies = currencyRepository.findAll().stream()
                .map(this::toCurrencyItem)
                .collect(Collectors.toList());
        List<DeploymentConfigDto.LocaleItem> locales = languageRepository.findAll().stream()
                .map(this::toLocaleItem)
                .collect(Collectors.toList());
        Map<String, String> userTypeToAccountType = deploymentConfig.getUserTypeToAccountType() == null
                ? Map.of()
                : deploymentConfig.getUserTypeToAccountType();

        return DeploymentConfigDto.builder()
                .countryCode(deploymentConfig.getCountryCode())
                .defaultCurrencyCode(deploymentConfig.getDefaultCurrencyCode())
                .defaultLocale(deploymentConfig.getDefaultLocale())
                .vehicleTerminology(deploymentConfig.getVehicleTerminology())
                .taxiOwnerLabel(deploymentConfig.getTaxiOwnerLabel())
                .currencies(currencies)
                .locales(locales)
                .userTypeToAccountType(userTypeToAccountType.isEmpty() ? null : userTypeToAccountType)
                .build();
    }

    private DeploymentConfigDto.CurrencyItem toCurrencyItem(Currency c) {
        return new DeploymentConfigDto.CurrencyItem(c.getIsoCode(), c.isActive());
    }

    private DeploymentConfigDto.LocaleItem toLocaleItem(Language l) {
        return new DeploymentConfigDto.LocaleItem(l.getLanguageKey(), l.getName());
    }
}
