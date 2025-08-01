package com.discphy.ad.domain.ad;

import java.time.LocalDateTime;

public class AdCommand {

    public record Create(
        String name,
        long rewardAmount,
        int joinCount,
        String description,
        String imageUrl,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        AdJoinConditionType type,
        String context
    ) {
    }

    public record Join(
        Long adId,
        Long userId
    ) {
        public static Join of(Long adId, Long userId) {
            return new Join(adId, userId);
        }
    }

    public record JoinedHistoriesQuery(
        Long userId,
        int page,
        int size
    ) {
        public static JoinedHistoriesQuery of(Long userId, int page, int size) {
            return new JoinedHistoriesQuery(userId, page, size);
        }

        public int page() {
            return Math.max(page - 1, 0);
        }

        public int size() {
            return Math.min(size, 50);
        }
    }
}
