package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthUserServiceImpl implements AuthUserService {

    @Override
    public AuthUser getAuthUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            return new AuthUser().setPrincipal(token.getName())
                    .setRoles(token.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        } else {
            return null;
        }
    }
}
