package org.temkarus0070.vkwallcleaner.services.datesParsers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextDatesParser implements DatesParser {

    private List<String> regexes = new ArrayList<>();

    public TextDatesParser() {
        regexes.add("");
        regexes.add("[0-9]+[ ]+января");
        regexes.add("[0-9]+[ ]+февраля");
        regexes.add("[0-9]+[ ]+марта");
        regexes.add("[0-9]+[ ]+апреля");
        regexes.add("[0-9]+[ ]+мая");
        regexes.add("[0-9]+[ ]+июня");
        regexes.add("[0-9]+[ ]+июля");
        regexes.add("[0-9]+[ ]+августа");
        regexes.add("[0-9]+[ ]+сентября");
        regexes.add("[0-9]+[ ]+октября");
        regexes.add("[0-9]+[ ]+ноября");
        regexes.add("[0-9]+[ ]+декабря");

    }

    @Override
    public List<LocalDate> getDates(String text, int postYear) {
        LocalDate now = LocalDate.now();
        Map<String, List<String>> regexesDates = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<String> allMatches = regexes.subList(1, 13)
                                         .stream()
                                         .filter(e -> {
                                             Pattern compilePattern = Pattern.compile(e);
                                             Matcher ourMatcher = compilePattern.matcher(text);
                                             return ourMatcher.find();
                                         })
                                         .toList();

        allMatches.forEach(e -> {
            Pattern compilePattern = Pattern.compile(e);
            Matcher ourMatcher = compilePattern.matcher(text);
            while (ourMatcher.find()) {
                regexesDates.putIfAbsent(e, new ArrayList<>());
                List<String> strings = regexesDates.get(e);
                strings.add(ourMatcher.group());
            }
        });
        try {
            if (!allMatches.isEmpty()) {
                return parseDate(regexesDates, text, postYear);
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return new ArrayList<>();

    }


    public List<LocalDate> parseDate(Map<String, List<String>> regexesDates, String text, int postYear) {
        List<LocalDate> localDateArrayList = new ArrayList<>();
        for (Map.Entry<String, List<String>> regexAndDate : regexesDates.entrySet()) {
            try {
                for (String date : regexAndDate.getValue()) {

                    String regex = regexAndDate.getKey();
                    int monthIndex = regexes.indexOf(regex);
                    Pattern numbersPattern = Pattern.compile("[0-9]+");
                    Matcher dayNumberMatcher = numbersPattern.matcher(date);
                    if (dayNumberMatcher.find()) {

                        Optional<LocalDate> extendDateFormat =
                            getExtendDateFormat(text, regex, date, Integer.parseInt(dayNumberMatcher.group(0)));
                        if (extendDateFormat.isPresent()) {
                            localDateArrayList.add(extendDateFormat.get());
                        } else {
                            localDateArrayList.add(LocalDate.of(postYear,
                                                                monthIndex,
                                                                Integer.parseInt(dayNumberMatcher.group(0))));
                        }
                    }
                }

            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
        return localDateArrayList;
    }

    private Optional<LocalDate> getExtendDateFormat(String text, String pattern, String shortDate, int dayNumber) {
        String extendPattern = pattern + "[ ]*[0-9]+";
        Pattern extendDatePattern = Pattern.compile(extendPattern);
        Matcher extendDateMatcher = extendDatePattern.matcher(text);
        Pattern numbersPattern = Pattern.compile("[0-9]+");

        while (extendDateMatcher.find()) {
            List<String> dates = new ArrayList<>();
            dates.add(extendDateMatcher.group(0));
            for (int i = 1; i < extendDateMatcher.groupCount(); i++) {
                dates.add(extendDateMatcher.group(i));
            }
            for (String date : dates) {

                Matcher dayNumberMatcher = numbersPattern.matcher(date);
                List<String> numbers = new ArrayList<>();
                while (dayNumberMatcher.find()) {

                    numbers.add(dayNumberMatcher.group());

                }
                List<Integer> nums = numbers.stream()
                                            .map(e -> Integer.parseInt(e))
                                            .sorted()
                                            .toList();
                if (dayNumberMatcher.find()) {

                    if (Integer.parseInt(dayNumberMatcher.group(0)) == dayNumber) {
                        return Optional.of(LocalDate.of(nums.get(nums.size() - 1), regexes.indexOf(pattern), dayNumber));

                    }

                }
            }
        }
        return Optional.empty();
    }

    private String doRegex(Optional<Month> exclude) {
        int limit = exclude.isPresent() ? exclude.get()
                                                 .getValue() : 13;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < limit - 1; i++) {
            stringBuilder.append("(" + regexes.get(i) + ")|");
        }
        stringBuilder.append("(" + regexes.get(limit - 1) + ")");
        return stringBuilder.toString();
    }
}
