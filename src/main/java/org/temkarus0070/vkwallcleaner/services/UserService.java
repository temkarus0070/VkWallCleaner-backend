package org.temkarus0070.vkwallcleaner.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public String getCurrentUserVkToken(){
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        OAuth2AuthenticationToken authentication1 = (OAuth2AuthenticationToken) authentication;
        return (String) authentication1.getPrincipal()
                                       .getAttributes()
                                       .get("vkToken");
    }
    public int getCurrentUserVkId(){
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        OAuth2AuthenticationToken authentication1 = (OAuth2AuthenticationToken) authentication;
        return Integer.parseInt((String) authentication1.getPrincipal()
                                       .getAttributes()
                                       .get("vkId"));
    }
}
