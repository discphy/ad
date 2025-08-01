package com.discphy.ad.domain.ad;

import java.time.LocalDateTime;
import java.util.List;

public class AdInfo {

    public record Create(
        Long id,
        String name,
        String description,
        String imageUrl,
        long rewardAmount
    ) {
        public static Create from(Ad ad) {
            return new Create(
                ad.getId(),
                ad.getName(),
                ad.getDescription(),
                ad.getImageUrl(),
                ad.getRewardAmount()
            );
        }
    }

    public record Join(
        Long adId,
        Long userId,
        LocalDateTime joinedAt
    ) {
        public static Join from(AdJoinedHistory joinedUser) {
            return new Join(
                joinedUser.getAdId(),
                joinedUser.getUserId(),
                joinedUser.getJoinedAt()
            );
        }
    }

    public record GetJoinAble(
        List<GetJoinAbleItem> items
    ) {
        public static GetJoinAble from(List<GetJoinAbleItem> items) {
            return new GetJoinAble(items);
        }
    }

    public record GetJoinAbleItem(
        Long adId,
        String name,
        String description,
        String imageUrl,
        long rewardAmount
    ) {
        public static GetJoinAbleItem from(Ad ad) {
            return new GetJoinAbleItem(
                ad.getId(),
                ad.getName(),
                ad.getDescription(),
                ad.getImageUrl(),
                ad.getRewardAmount()
            );
        }
    }

    public record GetJoinedHistories(
        Long userId,
        int page,
        int size,
        List<JoinedHistory> histories
    ) {
        public static GetJoinedHistories of(Long userId, int page, int size, List<JoinedHistory> histories) {
            return new GetJoinedHistories(
                userId,
                page + 1,
                size,
                histories
            );
        }
    }

    public record JoinedHistory(
        Long adId,
        Long userId,
        String name,
        LocalDateTime joinedAt,
        long rewardAmount
    ) {
        public static JoinedHistory from(AdJoinedHistory joinedUser) {
            return new JoinedHistory(
                joinedUser.getAdId(),
                joinedUser.getUserId(),
                joinedUser.getName(),
                joinedUser.getJoinedAt(),
                joinedUser.getRewardAmount()
            );
        }
    }
}
