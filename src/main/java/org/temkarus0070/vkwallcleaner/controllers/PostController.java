package org.temkarus0070.vkwallcleaner.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.temkarus0070.vkwallcleaner.Repositories.UserRepository;
import org.temkarus0070.vkwallcleaner.entities.User;
import org.temkarus0070.vkwallcleaner.entities.Wallpost;
import org.temkarus0070.vkwallcleaner.services.UserService;

@RestController
@AllArgsConstructor
public class PostController {

    UserRepository userRepository;

    UserService userService;
    @PostMapping("/posts/exclude")
    public void addExclusionPost(@RequestBody Wallpost wallpost) {
        User user = new User();
        user.setVkId(userService.getCurrentUserVkId());
        User updatedUser = userRepository.findById(userService.getCurrentUserVkId())
                                         .orElseGet(() -> user);
        updatedUser.getExclusionsPosts()
                   .add(wallpost);
        userRepository.save(updatedUser);
    }
}
