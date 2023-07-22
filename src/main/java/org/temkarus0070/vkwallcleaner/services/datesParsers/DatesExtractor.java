package org.temkarus0070.vkwallcleaner.services.datesParsers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatesExtractor {

    List<DatesParser> datesParsers;

    @Autowired
    public void setDatesParsers(List<DatesParser> datesParsers) {
        this.datesParsers = datesParsers;
    }

    public List<LocalDate> dates(String text, LocalDateTime postDate) {
        text = normalizeText(text);
        String finalText = text;
        List<LocalDate> collect = datesParsers.stream()
                                              .map(e -> e.getDates(finalText, postDate.getYear()))
                                              .flatMap(Collection::stream)
                                              .sorted(LocalDate::compareTo)
                                              .collect(Collectors.toList());
        if (!collect.isEmpty() && collect.stream()
                                         .max(LocalDate::compareTo)
                                         .get()
                                         .isBefore(postDate.toLocalDate())) {
            return collect.stream()
                          .map(e -> LocalDate.of(e.getYear() + 1, e.getMonthValue(), e.getDayOfMonth()))
                          .toList();
        }
        return collect;
    }

    private String normalizeText(String text) {
        text = text.replace("-го", "")
                   .replace("-ГО", "")
                   .replace("- го", "")
                   .replace("- ГО", "")
                   .replace("/", ".")
                   .replace("\\", ".")
                   .toLowerCase();

        text = text.replace("го", "");

        return text;
    }
}
