package org.temkarus0070.vkwallcleaner.services;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.temkarus0070.vkwallcleaner.Repositories.UserRepository;
import org.temkarus0070.vkwallcleaner.entities.User;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;

    public String getCurrentUserVkToken() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        JwtAuthenticationToken authentication1 = (JwtAuthenticationToken) authentication;
        return (String) authentication1.getTokenAttributes()
                                       .get("vkToken");
    }

    public int getCurrentUserVkId() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        JwtAuthenticationToken authentication1 = (JwtAuthenticationToken) authentication;
        return Integer.parseInt((String) authentication1.getTokenAttributes()
                                                        .get("vkId"));
    }

    public Optional<User> getCurrentUser(int currentUserVkId) {
        return userRepository.findById(currentUserVkId);
    }

    public void saveUser(User user) {
        this.userRepository.save(user);
    }
}
