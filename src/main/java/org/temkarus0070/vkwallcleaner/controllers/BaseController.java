package org.temkarus0070.vkwallcleaner.controllers;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.temkarus0070.vkwallcleaner.services.TrashWallRemover;

@RestController
@AllArgsConstructor
public class BaseController {

    private TrashWallRemover trashWallRemover;

    @PostMapping("/clean-current")
    public int cleanCurrentPosts() throws ClientException, ApiException {
        return trashWallRemover.removeCurrentGiveaways();
    }

    @PostMapping("/clean-past")
    public int cleanPastPosts() throws ClientException, ApiException {
       return trashWallRemover.removePastGiveaways();
    }
}
