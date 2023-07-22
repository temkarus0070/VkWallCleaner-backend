package org.temkarus0070.vkwallcleaner.services;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.wall.GetFilter;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
public class UserWallParser {

    private final UserService userService;

    public UserWallParser(UserService userService) {
        this.userService = userService;
    }

    public Map.Entry<VkApiClient, UserActor> buildClient() {

        int ID = userService.getCurrentUserVkId();
        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);

        String vkToken = userService.getCurrentUserVkToken();
        UserActor actor = new UserActor(ID, vkToken);
        return Map.entry(vk, actor);
    }

    public List<WallpostFull> findWallPosts(List<Predicate<WallpostFull>> predicates) throws ClientException, ApiException {
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        int limit = 1000;
        int offset = 0;
        List<WallpostFull> wallposts = new ArrayList<>();
        GetResponse execute;
        while (!(execute = vkApiClientUserActor.getKey()
                                               .wall()
                                               .get(vkApiClientUserActor.getValue())
                                               .count(limit)
                                               .offset(offset)
                                               .filter(GetFilter.ALL)
                                               .execute()).getItems()
                                                          .isEmpty() && !execute.getItems()
                                                                                .isEmpty()) {
            offset += execute.getItems()
                             .size();
            execute.getItems()
                   .forEach(post -> {
                       if (predicates.stream()
                                     .anyMatch(e -> e.test(post))) {
                           wallposts.add(post);
                       }
                   });
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return wallposts;

    }
}
