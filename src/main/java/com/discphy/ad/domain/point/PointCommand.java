package com.discphy.ad.domain.point;

public class PointCommand {

    public record Reward(
        Long userId,
        Long amount
    ) {
        public static Reward of(Long userId, Long amount) {
            return new Reward(userId, amount);
        }
    }
}
