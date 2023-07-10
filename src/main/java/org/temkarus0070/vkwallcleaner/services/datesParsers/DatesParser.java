package org.temkarus0070.vkwallcleaner.services.datesParsers;

import java.time.LocalDate;
import java.util.List;

public interface DatesParser {
    public List<LocalDate> getDates(String text, int postYear);
}
