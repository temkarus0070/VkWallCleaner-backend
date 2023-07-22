package org.temkarus0070.vkwallcleaner.services.datesParsers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NumericDatesParser implements DatesParser {

    private static int MIN_YEAR = 2018;

    @Override
    public List<LocalDate> getDates(String text, int postYear) {
        List<LocalDate> localDates = new ArrayList<>();
        Pattern datePattern = Pattern.compile("[0-9]+\\.[0-9]+\\.?[0-9]*[ ]*", Pattern.CASE_INSENSITIVE);
        Matcher datePatternMatcher = datePattern.matcher(text);

        try {

            while (datePatternMatcher.find()) {

                String group = datePatternMatcher.group();
                List<String> monthsDates = new ArrayList<>();
                monthsDates.add(group);

                List<String> correctMonths = monthsDates.stream()
                                                        .filter(e -> {
                                                            try {
                                                                Month month = getMonth(e);
                                                                if (month != null) {
                                                                    return true;
                                                                } else {
                                                                    return false;
                                                                }
                                                            } catch (Exception ex) {
                                                                return false;
                                                            }
                                                        })
                                                        .toList();
                if (!correctMonths.isEmpty()) {
                    localDates.addAll(correctMonths.stream()
                                                   .map(e -> this.parseDate(e, postYear))
                                                   .filter(e -> !e.equals(LocalDate.MIN))
                                                   .toList());
                }

            }
            return localDates;
        } catch (Exception ex) {
            System.out.println(ex);
            return localDates;
        }
    }

    private LocalDate parseDate(String date, int postYear) {
        Pattern extendDatePattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");
        Matcher extendDateMatcher = extendDatePattern.matcher(date);
        String[] dateParts = date.split("\\.");
        try {

            if (extendDateMatcher.find() && Integer.parseInt(dateParts[2].trim()) >= MIN_YEAR) {
                return LocalDate.of(Integer.parseInt(dateParts[2].trim()),
                                    Integer.parseInt(dateParts[1].trim()),
                                    Integer.parseInt(dateParts[0].trim()));
            } else {
                return LocalDate.of(postYear, Integer.parseInt(dateParts[1].trim()), Integer.parseInt(dateParts[0].trim()));
            }
        } catch (Exception ex) {
            return LocalDate.MIN;
        }
    }

    private boolean isValidDate(String date) {
        return true;
    }


    private static Month getMonth(String date) {
        if (!date.contains("00")) {
            Pattern compile = Pattern.compile("[0-9]+\\.[0-9]+");
            Matcher matcher = compile.matcher(date);
            if (matcher.find()) {
                String group = matcher.group(0)
                                      .trim();
                String[] split = group.split("\\.");
                return Month.of(Integer.parseInt(split[1]));
            }
            return null;
        }
        return null;
    }
}
