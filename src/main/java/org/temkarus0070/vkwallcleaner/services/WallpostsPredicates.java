package org.temkarus0070.vkwallcleaner.services;

import com.vk.api.sdk.objects.wall.Wallpost;
import com.vk.api.sdk.objects.wall.WallpostFull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.temkarus0070.vkwallcleaner.entities.User;
import org.temkarus0070.vkwallcleaner.services.datesParsers.DatesExtractor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

enum PredicateType {
    ALL, CURRENT_YEAR_GIVEAWAYS
}

@Component
public class WallpostsPredicates {

    private final UserService userService;

    private final DatesExtractor datesExtractor;
    private final Map<PredicateType, List<Predicate<WallpostFull>>> predicatesMap = new HashMap<>();
    @Value("#{'${application.cleaner.giveaways-words}'.split(',')}")
    private List<String> giveawaysWords;

    public WallpostsPredicates(UserService userService, DatesExtractor datesExtractor) {
        this.userService = userService;
        this.datesExtractor = datesExtractor;
        setPredicatesMap();
    }

    public Map<PredicateType, List<Predicate<WallpostFull>>> getPredicatesMap() {
        return Map.copyOf(predicatesMap);
    }

    private void setPredicatesMap() {

        List<Predicate<WallpostFull>> predicates = new ArrayList<>();
        predicates.add(post -> {

            LocalDate now = LocalDate.now();
            User user = this.userService.getCurrentUser(this.userService.getCurrentUserVkId())
                                        .orElseGet(User::new);
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

            LocalDate now = LocalDate.now();
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
        predicatesMap.put(PredicateType.CURRENT_YEAR_GIVEAWAYS, predicates);

        predicates = new ArrayList<>(predicatesMap.get(PredicateType.CURRENT_YEAR_GIVEAWAYS));
        predicates.add(e -> {
            if (e.getCopyHistory() != null && !e.getCopyHistory()
                                                .isEmpty() && giveawaysWords.stream()
                                                                            .anyMatch(e.getCopyHistory()
                                                                                       .get(0)
                                                                                       .getText()
                                                                                       .toLowerCase()::contains)) {
                return true;
            }
            return false;
        });
        predicatesMap.put(PredicateType.ALL, predicates);
    }

}
