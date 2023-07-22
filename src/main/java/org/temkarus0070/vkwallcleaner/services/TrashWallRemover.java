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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
public class TrashWallRemover {

    private final UserService userService;

    private final DatesExtractor datesExtractor;

    private final UserWallParser userWallParser;

    @Value("#{'${application.cleaner.giveaways-words}'.split(',')}")
    private List<String> giveawaysWords;

    public TrashWallRemover(UserService userService, DatesExtractor datesExtractor, UserWallParser userWallParser) {
        this.userService = userService;
        this.datesExtractor = datesExtractor;
        this.userWallParser = userWallParser;
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

    public int removeCurrentGiveaways() throws ClientException, ApiException {
        LocalDate now = LocalDate.now();
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        List<Predicate<WallpostFull>> predicates = new ArrayList<>();
        User user = this.userService.getCurrentUser(this.userService.getCurrentUserVkId())
                                    .orElseGet(User::new);

        predicates.add(post -> {
            List<Wallpost> copyHistory = post.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                String text = repost.getText()
                                    .toLowerCase();
                if (user.getExclusionsPosts()
                        .stream()
                        .map(org.temkarus0070.vkwallcleaner.entities.Wallpost::getText)
                        .map(String::toLowerCase)
                        .noneMatch(text::contains)) {
                    LocalDateTime postDate = LocalDateTime.ofEpochSecond(post.getDate(), 0, ZoneOffset.ofHours(3));
                    List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                    if (!parsedDates.isEmpty()) {
                        if (parsedDates.stream()
                                       .max(LocalDate::compareTo)
                                       .get()
                                       .isBefore(now)) {
                            return true;
                        }
                    }
                }

            }
            return false;
        });

        predicates.add(wallpostFull -> {
            LocalDateTime postDate = LocalDateTime.ofEpochSecond(wallpostFull.getDate(), 0, ZoneOffset.ofHours(3));
            List<Wallpost> copyHistory = wallpostFull.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                String text = repost.getText()
                                    .toLowerCase();
                List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                if (parsedDates.isEmpty() && text.contains("завтра") && giveawaysWords.stream()
                                                                                      .anyMatch(text::contains) &&
                        postDate.toLocalDate()
                                .datesUntil(now)
                                .count() >= 2) {
                    return true;
                }

            }
            return false;
        });

        predicates.add(wallpostFull -> {
            List<Wallpost> copyHistory = wallpostFull.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                if (repost.getIsDeleted() != null && repost.getIsDeleted()) {
                    return true;
                }
            }
            return false;
        });

        List<WallpostFull> wallPosts = userWallParser.findWallPosts(predicates);

        return removePosts(wallPosts, vkApiClientUserActor);

    }

    // TODO refactor case when giveaway ends on february when we clean in december previous year
    public int removePastGiveaways() throws ClientException, ApiException {
        LocalDate now = LocalDate.now();
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        List<Predicate<WallpostFull>> predicates = new ArrayList<>();
        User user = this.userService.getCurrentUser(this.userService.getCurrentUserVkId())
                                    .orElseGet(User::new);

        predicates.add(post -> {
            List<Wallpost> copyHistory = post.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                String text = repost.getText()
                                    .toLowerCase();
                if (user.getExclusionsPosts()
                        .stream()
                        .map(org.temkarus0070.vkwallcleaner.entities.Wallpost::getText)
                        .map(String::toLowerCase)
                        .noneMatch(text::contains)) {
                    LocalDateTime postDate = LocalDateTime.ofEpochSecond(post.getDate(), 0, ZoneOffset.ofHours(3));
                    List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                    if (!parsedDates.isEmpty() && postDate.getYear() < now.getYear()) {
                        if (parsedDates.stream()
                                       .max(LocalDate::compareTo)
                                       .get()
                                       .isBefore(now)) {
                            return true;
                        }
                    }
                }

            }
            return false;
        });

        predicates.add(wallpostFull -> {
            LocalDateTime postDate = LocalDateTime.ofEpochSecond(wallpostFull.getDate(), 0, ZoneOffset.ofHours(3));
            List<Wallpost> copyHistory = wallpostFull.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                String text = repost.getText()
                                    .toLowerCase();
                List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                if (parsedDates.isEmpty() && text.contains("завтра") && giveawaysWords.stream()
                                                                                      .anyMatch(text::contains) &&
                        postDate.toLocalDate()
                                .datesUntil(now)
                                .count() >= 2) {
                    return true;
                }

            }
            return false;
        });
        predicates.add(wallpostFull -> {
            LocalDateTime postDate = LocalDateTime.ofEpochSecond(wallpostFull.getDate(), 0, ZoneOffset.ofHours(3));
            List<Wallpost> copyHistory = wallpostFull.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                String text = repost.getText()
                                    .toLowerCase();
                List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                if (parsedDates.isEmpty() && giveawaysWords.stream()
                                                           .anyMatch(text::contains)) {
                    return true;
                }

            }
            return false;
        });

        predicates.add(wallpostFull -> {
            List<Wallpost> copyHistory = wallpostFull.getCopyHistory();
            if (copyHistory != null && !copyHistory.isEmpty()) {
                Wallpost repost = copyHistory.get(0);
                if (repost.getIsDeleted() != null && repost.getIsDeleted()) {
                    return true;
                }
            }
            return false;
        });

        List<WallpostFull> wallPosts = userWallParser.findWallPosts(predicates);

        return removePosts(wallPosts, vkApiClientUserActor);
    }
}
