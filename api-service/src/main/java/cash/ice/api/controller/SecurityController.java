package cash.ice.api.controller;

import cash.ice.api.dto.SortInput;
import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.sqldb.entity.SecurityGroup;
import cash.ice.sqldb.entity.SecurityRight;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.io.Serializable;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SecurityController {
    private static final JsonToMapConverter JSON_TO_MAP_CONVERTER = new JsonToMapConverter();

    private final SecurityGroupRepository securityGroupRepository;

    @QueryMapping
    public Iterable<SecurityGroup> allSecurityGroups(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return securityGroupRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @SchemaMapping(typeName = "SecurityGroup", field = "metaData")
    public String securityGroupMeta(SecurityGroup securityGroup) {
        return JSON_TO_MAP_CONVERTER.convertToDatabaseColumn((Serializable) securityGroup.getMeta());
    }

    @SchemaMapping(typeName = "SecurityGroup", field = "rightsList")
    public List<String> securityGroupRights(SecurityGroup securityGroup) {
        return securityGroup.getRights().stream().map(SecurityRight::getName).toList();
    }
}
