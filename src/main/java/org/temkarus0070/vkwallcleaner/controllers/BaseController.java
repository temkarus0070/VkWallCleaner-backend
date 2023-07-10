package org.temkarus0070.vkwallcleaner.controllers;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.temkarus0070.vkwallcleaner.services.TrashWallRemover;

@RestController
public class BaseController {

    private TrashWallRemover trashWallRemover;

    @Autowired
    public void setTrashWallRemover(TrashWallRemover trashWallRemover) {
        this.trashWallRemover = trashWallRemover;
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
