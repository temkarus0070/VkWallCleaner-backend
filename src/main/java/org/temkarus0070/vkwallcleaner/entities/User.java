package org.temkarus0070.vkwallcleaner.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    private int vkId;

    @ElementCollection
    @CollectionTable(name = "EXCLUSIONS_POSTS")
    private Collection<Wallpost> exclusionsPosts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ACTIVE_REMOVED_POSTS")
    private Collection<Wallpost> activeRemovedGiveawaysPosts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "GIVEAWAYS_SOURCES")
    @Column(name = "user_id")
    private Collection<Integer> usersIdsToSearchGiveawaysFrom = new ArrayList<>();
}
