package org.temkarus0070.vkwallcleaner.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collection;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    private int vkId;

    @ElementCollection()
    @BatchSize(size = 15)
    @CollectionTable(name = "PROBABLE_TRASH_POSTS")
    private Collection<Wallpost> probableTrashPost = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "EXCLUSIONS_POSTS")
    private Collection<Wallpost> exclusionsPosts = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "USER_DELETED_POST_GROUPS")
    private Collection<DeletedPostGroup> deletedPostGroups = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "GIVEAWAYS_SOURCES")
    @Column(name = "user_id")
    private Collection<Integer> usersIdsToSearchGiveawaysFrom = new ArrayList<>();
}
