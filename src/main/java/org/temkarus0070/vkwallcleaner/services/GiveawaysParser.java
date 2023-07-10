package org.temkarus0070.vkwallcleaner.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GiveawaysParser {

    @Value("#{'${application.search.giveaways-words}'.split(',')}")
    private List<String> giveawaysWords ;
}
