package com.reddit.spring.model;

import com.reddit.spring.exception.types.RedditException;

import java.util.Arrays;

public enum VoteType {
    UPVOTE(1), DOWNVOTE(-1);

    private int direction;

    VoteType(int direction) {
    }

    public static VoteType lookup(Integer direction) {
        return Arrays.stream(VoteType.values())
                .filter(value -> value.getDirection().equals(direction))
                .findAny()
                .orElseThrow(() -> new RedditException("vote cannot be found"));
    }

    public Integer getDirection() {
        return direction;
    }
}
