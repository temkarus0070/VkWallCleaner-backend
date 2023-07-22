package org.temkarus0070.vkwallcleaner.services;

import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.wall.Wallpost;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.queries.wall.WallDeleteQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.temkarus0070.vkwallcleaner.entities.User;
import org.temkarus0070.vkwallcleaner.services.datesParsers.DatesExtractor;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class TrashWallRemover {

    private final UserService userService;

    private final DatesExtractor datesExtractor;

    private final UserWallParser userWallParser;

    @Value("#{'${application.cleaner.giveaways-words}'.split(',')}")
    private List<String> giveawaysWords;

    private WallpostsPredicates wallpostsPredicates;

    public TrashWallRemover(UserService userService,
                            DatesExtractor datesExtractor,
                            UserWallParser userWallParser,
                            WallpostsPredicates wallpostsPredicates) {
        this.userService = userService;
        this.datesExtractor = datesExtractor;
        this.userWallParser = userWallParser;
        this.wallpostsPredicates = wallpostsPredicates;
    }

    public Map.Entry<VkApiClient, UserActor> buildClient() {

        int ID = userService.getCurrentUserVkId();
        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);

        String vkToken = userService.getCurrentUserVkToken();
        UserActor actor = new UserActor(ID, vkToken);
        return Map.entry(vk, actor);
    }

    private static int removePosts(Collection<WallpostFull> wallposts, Map.Entry<VkApiClient, UserActor> vkApiClientUserActor) {
        int count = 0;
        List<AbstractQueryBuilder> abstractQueryBuilders = new ArrayList<>();
        try {
            for (WallpostFull wallpost : wallposts) {
                WallDeleteQuery wallDeleteQuery = vkApiClientUserActor.getKey()
                                                                      .wall()
                                                                      .delete(vkApiClientUserActor.getValue())
                                                                      .postId(wallpost.getId());
                abstractQueryBuilders.add(wallDeleteQuery);
                if (abstractQueryBuilders.size() == 25) {
                    vkApiClientUserActor.getKey()
                                        .execute()
                                        .batch(vkApiClientUserActor.getValue(), abstractQueryBuilders)
                                        .execute();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    abstractQueryBuilders.clear();
                }

                count++;

            }
            if (!abstractQueryBuilders.isEmpty()) {
                vkApiClientUserActor.getKey()
                                    .execute()
                                    .batch(vkApiClientUserActor.getValue(), abstractQueryBuilders)
                                    .execute();
            }

        } catch (Exception ex) {
            System.out.println(ex);
        }
        return count;
    }

    public Map<String, List<LocalDate>> getRemovedPosts() {
        Optional<User> currentUser = userService.getCurrentUser(userService.getCurrentUserVkId());
        Collection<org.temkarus0070.vkwallcleaner.entities.Wallpost> activeRemovedGiveawaysPosts = currentUser.orElse(new User())
                                                                                                              .getActiveRemovedGiveawaysPosts();
        return activeRemovedGiveawaysPosts.stream()
                                          .map(post -> Map.entry(datesExtractor.dates(post.getText(), LocalDateTime.now()), post))
                                          .collect(Collectors.toMap(e -> "https://vk.com/wall" + e.getValue()
                                                                                                  .getAuthorId() + "_"
                                                                             + e.getValue()
                                                                                .getPostId(), Map.Entry::getKey));
    }

    public List<URI> getAndRemoveAllGiveaways() throws ClientException, ApiException {
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        List<Predicate<WallpostFull>> predicates = wallpostsPredicates.getPredicatesMap()
                                                                      .get(PredicateType.ALL);

        List<WallpostFull> wallPosts = userWallParser.findWallPosts(predicates);

          removePosts(wallPosts, vkApiClientUserActor);
        User user = userService.getCurrentUser(userService.getCurrentUserVkId())
                               .orElse(new User());
        user.getActiveRemovedGiveawaysPosts()
            .addAll(wallPosts.stream()
                             .filter(e -> !e.getCopyHistory()
                                            .isEmpty())
                             .map(e -> {
                                 Wallpost wallpost = e.getCopyHistory()
                                                      .get(0);
                                 return new org.temkarus0070.vkwallcleaner.entities.Wallpost(wallpost.getId(),
                                                                                             wallpost.getFromId(),
                                                                                             wallpost.getText());
                             })
                             .toList());
        userService.saveUser(user);
        return wallPosts.stream()
                        .map(e -> URI.create("https://vk.com/wall" + e.getCopyHistory()
                                                                      .get(0)
                                                                      .getFromId() + "_" + e.getCopyHistory()
                                                                                            .get(0)
                                                                                            .getId()))
                        .toList();
    }

    public int removeCurrentGiveaways() throws ClientException, ApiException {
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        List<Predicate<WallpostFull>> predicates = wallpostsPredicates.getPredicatesMap()
                                                                      .get(PredicateType.CURRENT_YEAR_GIVEAWAYS);

        List<WallpostFull> wallPosts = userWallParser.findWallPosts(predicates);
        return removePosts(wallPosts, vkApiClientUserActor);

    }

    // TODO refactor case when giveaway ends on february when we clean in december previous year
    public int removePastGiveaways() throws ClientException, ApiException {
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        List<Predicate<WallpostFull>> predicates = wallpostsPredicates.getPredicatesMap()
                                                                      .get(PredicateType.LAST_YEARS);

        List<WallpostFull> wallPosts = userWallParser.findWallPosts(predicates);

        return removePosts(wallPosts, vkApiClientUserActor);
    }
}
