package com.discphy.ad.domain.ad;

import java.time.LocalDateTime;

public class AdEvent {

    public record Joined(
        Long adId,
        Long userId,
        long rewardAmount,
        LocalDateTime joinedAt
    ) {
        public static Joined of(AdJoinedHistory joinedUser) {
            return new Joined(
                joinedUser.getAdId(),
                joinedUser.getUserId(),
                joinedUser.getRewardAmount(),
                joinedUser.getJoinedAt()
            );
        }
    }
}
