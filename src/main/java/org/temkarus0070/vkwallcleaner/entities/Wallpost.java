package org.temkarus0070.vkwallcleaner.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;

@Embeddable
public class Wallpost {
    private int postId;
    private String text;
}
