package org.temkarus0070.vkwallcleaner.controllers;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.temkarus0070.vkwallcleaner.Repositories.UserRepository;
import org.temkarus0070.vkwallcleaner.entities.User;
import org.temkarus0070.vkwallcleaner.entities.Wallpost;
import org.temkarus0070.vkwallcleaner.services.TrashWallRemover;
import org.temkarus0070.vkwallcleaner.services.UserService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/posts")
public class PostController {

    UserRepository userRepository;


    TrashWallRemover trashWallRemover;

    UserService userService;

    @PostMapping("/get-and-clean-all")
    public List<URI> getAndCleanAllPosts() throws ClientException, ApiException {
        return trashWallRemover.getAndRemoveAllGiveaways();
    }

    @GetMapping("/removed-posts")
    public Map<String, List<LocalDate>> getRemovedPosts() {
        return trashWallRemover.getRemovedPosts();
    }

    @PostMapping("/exclude")
    public void addExclusionPost(@RequestBody Wallpost wallpost) {
        User user = new User();
        user.setVkId(userService.getCurrentUserVkId());
        User updatedUser = userRepository.findById(userService.getCurrentUserVkId())
                                         .orElseGet(() -> user);
        updatedUser.getExclusionsPosts()
                   .add(wallpost);
        userRepository.save(updatedUser);
    }

    @PostMapping("/clean-current")
    public int cleanCurrentPosts() throws ClientException, ApiException {
        return trashWallRemover.removeCurrentGiveaways();
    }

    @PostMapping("/clean-past")
    public int cleanPastPosts() throws ClientException, ApiException {
        return trashWallRemover.removePastGiveaways();
    }
}
