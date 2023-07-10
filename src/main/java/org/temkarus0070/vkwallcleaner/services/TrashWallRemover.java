package org.temkarus0070.vkwallcleaner.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.wall.GetFilter;
import com.vk.api.sdk.objects.wall.Wallpost;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.queries.wall.WallDeleteQuery;
import org.temkarus0070.vkwallcleaner.services.datesParsers.DatesExtractor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class TrashWallRemover {

    private UserService userService;

    private DatesExtractor datesExtractor;

    @Value("#{'${application.cleaner.giveaways-words}'.split(',')}")
    private List<String> giveawaysWords;


    private static List<String> exclusion = new ArrayList<>();

    public TrashWallRemover(UserService userService, DatesExtractor datesExtractor) {
        this.userService = userService;
        this.datesExtractor = datesExtractor;
    }

    static {
        exclusion.add(
            "Новый РОЗЫГРЫШ от Суши Мастера ✨\n" + "Спеши принять участие!\n" + "\n" + "Дарим 5 сетов Ажиотаж лайт \uD83E\uDD29\n"
                + "5 кг роллов = 5 победителей!\n" + "\n" + "✨Как принять участие:\n"
                + "1. Проверь подписку на сообщество https://vk.com/sm_voronezh\n"
                + "2. Подпишись на рассылку с акциями и секретными промокодами — ищи в меню вкладку «акции»\n"
                + "3. Делай репост этого поста к себе на страничку\n" + "4. Ставь лайк на 3 предыдущих поста\n"
                + "5. Пиши любой комментарий под этим постом. ‼️Больше комментариев, больше шансов выиграть!\n" + "\n"
                + "30 июня выберем пять счастливчиков через независимый сервис https://konkurzilla.ru, каждый из которых получит сет Ажиотаж лайт!\n"
                + "Удачи");
    }

    public Map.Entry<VkApiClient, UserActor> buildClient() {

        int ID = userService.getCurrentUserVkId();
        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);

        String vkToken = System.getenv(userService.getCurrentUserVkToken());
        UserActor actor = new UserActor(ID, vkToken);
        return Map.entry(vk, actor);
    }

    public int removeCurrentGiveaways() throws ClientException, ApiException {
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int limit = 1000;
        int offset = 0;
        Map<Month, List<Map.Entry<WallpostFull, List<LocalDate>>>> postsByMonths = new TreeMap<>();

        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> posts = new TreeMap<>();
        GetResponse execute;
        AtomicBoolean hasOld = new AtomicBoolean();
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> probableTrash = new TreeMap<>();
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> probableTomorrowTrash = new TreeMap<>();
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> deletedReposts = new TreeMap<>();
        while (!(execute = vkApiClientUserActor.getKey()
                                               .wall()
                                               .get(vkApiClientUserActor.getValue())
                                               .count(limit)
                                               .offset(offset)
                                               .filter(GetFilter.ALL)
                                               .execute()).getItems()
                                                          .isEmpty() && !execute.getItems()
                                                                                .isEmpty() && !hasOld.get()) {
            offset += execute.getItems()
                             .size();
            execute.getItems()
                   .forEach(post -> {
                       LocalDateTime postDate = LocalDateTime.ofEpochSecond(post.getDate(), 0, ZoneOffset.ofHours(3));
                       if (postDate.getYear() != currentYear) {
                           hasOld.set(true);
                           return;
                       }
                       List<Wallpost> copyHistory = post.getCopyHistory();
                       if (copyHistory != null && !copyHistory.isEmpty()) {
                           Wallpost repost = copyHistory.get(0);
                           String text = repost.getText()
                                               .toLowerCase();
                           if (exclusion.stream()
                                        .map(String::toLowerCase)
                                        .noneMatch(text::contains)) {
                               List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                               String rusCurrentMonthName = Month.of(now.getMonthValue())
                                                                 .getDisplayName(TextStyle.FULL_STANDALONE,
                                                                                 Locale.forLanguageTag("ru"));

                               String rusMonthNameTwo = Calendar.getInstance()
                                                                .getDisplayName(Calendar.MONTH,
                                                                                Calendar.LONG_FORMAT,
                                                                                new Locale("ru"));
                               if (!parsedDates.isEmpty()) {
                                   if (parsedDates.stream()
                                                  .max(LocalDate::compareTo)
                                                  .get()
                                                  .isBefore(now)) {
                                       posts.put(post.getId(), Map.entry(post, parsedDates));
                                   }
                               } else {
                                   if (text.contains("завтра") && giveawaysWords.stream()
                                                                                .anyMatch(text::contains) &&
                                           postDate.toLocalDate()
                                                   .datesUntil(now)
                                                   .count() >= 2) {
                                       probableTomorrowTrash.put(post.getId(), Map.entry(post, new ArrayList<>()));
                                   } else if (repost.getIsDeleted() != null && repost.getIsDeleted()) {
                                       deletedReposts.put(post.getId(), Map.entry(post, new ArrayList<>()));
                                   }
                               }
                           }

                       }
                   });
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        int count = removePosts(posts.values(), vkApiClientUserActor);
        count += removePosts(probableTomorrowTrash.values(), vkApiClientUserActor);
        count += removePosts(deletedReposts.values(), vkApiClientUserActor);
        return count;

    }

    private Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> getProbableTrash(Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> posts,
                                                                                    int excludeMonth) {
        return posts.entrySet()
                    .stream()
                    .filter(e -> {
                        List<LocalDate> dates = e.getValue()
                                                 .getValue();
                        return (dates.stream()
                                     .anyMatch(e1 -> e1.getMonthValue() < excludeMonth) && dates.stream()
                                                                                                .anyMatch(e1 -> e1.getMonthValue()
                                                                                                                    >= Math.max(
                                                                                                    excludeMonth,
                                                                                                    12)));
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> getExactTrash(Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> posts,
                                                                                 int excludeMonth) {
        return posts.entrySet()
                    .stream()
                    .filter(e -> {
                        List<LocalDate> dates = e.getValue()
                                                 .getValue();
                        return (dates.stream()
                                     .allMatch(e1 -> e1.getMonthValue() < excludeMonth));
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    private static int removePosts(Collection<Map.Entry<WallpostFull, List<LocalDate>>> wallposts,
                                   Map.Entry<VkApiClient, UserActor> vkApiClientUserActor) {
        int count = 0;
        List<AbstractQueryBuilder> abstractQueryBuilders = new ArrayList<>();
        try {
            for (Map.Entry<WallpostFull, List<LocalDate>> wallpost : wallposts) {
                WallDeleteQuery wallDeleteQuery = vkApiClientUserActor.getKey()
                                                                      .wall()
                                                                      .delete(vkApiClientUserActor.getValue())
                                                                      .postId(wallpost.getKey()
                                                                                      .getId());
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

    public int removePastGiveaways() throws ClientException, ApiException {
        Map.Entry<VkApiClient, UserActor> vkApiClientUserActor = buildClient();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int limit = 1000;
        int offset = 0;
        Map<Month, List<Map.Entry<WallpostFull, List<LocalDate>>>> postsByMonths = new TreeMap<>();

        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> posts = new TreeMap<>();
        GetResponse execute;
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> probableTrash = new TreeMap<>();
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> giveawaysTrash = new TreeMap<>();
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> probableTomorrowTrash = new TreeMap<>();
        Map<Integer, Map.Entry<WallpostFull, List<LocalDate>>> deletedReposts = new TreeMap<>();
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
                       LocalDateTime postDate = LocalDateTime.ofEpochSecond(post.getDate(), 0, ZoneOffset.ofHours(3));
                       if (postDate.getYear() != currentYear) {

                           List<Wallpost> copyHistory = post.getCopyHistory();
                           if (copyHistory != null && !copyHistory.isEmpty()) {
                               Wallpost repost = copyHistory.get(0);
                               String text = repost.getText()
                                                   .toLowerCase();
                               List<LocalDate> parsedDates = datesExtractor.dates(text, postDate);
                               if (!parsedDates.isEmpty()) {
                                   if (parsedDates.stream()
                                                  .max(LocalDate::compareTo)
                                                  .get()
                                                  .isBefore(now)) {
                                       posts.put(post.getId(), Map.entry(post, parsedDates));
                                   }
                               } else {
                                   if (text.contains("завтра") && giveawaysWords.stream()
                                                                                .anyMatch(text::contains) &&
                                           postDate.toLocalDate()
                                                   .datesUntil(now)
                                                   .count() >= 2) {
                                       probableTomorrowTrash.put(post.getId(), Map.entry(post, new ArrayList<>()));
                                   } else if (repost.getIsDeleted() != null && repost.getIsDeleted()) {
                                       deletedReposts.put(post.getId(), Map.entry(post, new ArrayList<>()));
                                   } else if (giveawaysWords.stream()
                                                            .anyMatch(text::contains)) {
                                       giveawaysTrash.put(post.getId(), Map.entry(post, new ArrayList<>()));
                                   }
                               }
                           }
                       }
                   });
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        probableTrash.putAll(getProbableTrash(posts, 13));
        int count = removePosts(posts.values(), vkApiClientUserActor);
        count += removePosts(probableTomorrowTrash.values(), vkApiClientUserActor);
        count += removePosts(deletedReposts.values(), vkApiClientUserActor);
        count += removePosts(giveawaysTrash.values(), vkApiClientUserActor);
        return count;
    }
}
