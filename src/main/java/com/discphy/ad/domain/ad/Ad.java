package com.discphy.ad.domain.ad;

import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "ad",
    indexes = {
        @Index(name = "idx_ad_joinable", columnList = "join_count, started_at, ended_at, reward_amount DESC"),
    }
)
public class Ad {

    public static final long MAX_REWARD_AMOUNT = 1_000_000L;
    public static final int MAX_JOIN_COUNT = 100;

    @Id
    @Column(name = "ad_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
    private long rewardAmount;
    private int joinCount;
    private String description;
    private String imageUrl;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    private AdJoinConditionType type;
    private String context;

    @Builder
    private Ad(Long id,
               String name,
               long rewardAmount,
               int joinCount,
               String description,
               String imageUrl,
               LocalDateTime startedAt,
               LocalDateTime endedAt,
               AdJoinConditionType type,
               String context) {
        validateName(name);
        validateRewardAmount(rewardAmount);
        validateJoinCount(joinCount);
        validatePeriod(startedAt, endedAt);

        this.id = id;
        this.name = name;
        this.rewardAmount = rewardAmount;
        this.joinCount = joinCount;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.type = type;
        this.context = context;
    }

    public static Ad create(AdCommand.Create command) {
        return Ad.builder()
            .name(command.name())
            .rewardAmount(command.rewardAmount())
            .joinCount(command.joinCount())
            .description(command.description())
            .imageUrl(command.imageUrl())
            .startedAt(command.startedAt())
            .endedAt(command.endedAt())
            .type(command.type())
            .context(command.context())
            .build();
    }

    public void join() {
        if (joinCount <= 0) {
            throw new CoreException(ErrorType.CONFLICT, "참여가 불가능합니다.");
        }

        joinCount--;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "광고명은 필수입니다.");
        }
    }

    private void validateRewardAmount(long rewardAmount) {
        if (rewardAmount < 0 || rewardAmount > MAX_REWARD_AMOUNT) {
            throw new CoreException(ErrorType.BAD_REQUEST, "적립 액수가 유효하지 않습니다.");
        }
    }

    private void validateJoinCount(int joinCount) {
        if (joinCount <= 0 || joinCount > MAX_JOIN_COUNT) {
            throw new CoreException(ErrorType.BAD_REQUEST, "참여 가능 회수가 유효하지 않습니다.");
        }
    }

    private void validatePeriod(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "노출 기간은 필수입니다.");
        }

        if (endedAt.isBefore(startedAt)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "노출 기간이 유효하지 않습니다.");
        }
    }
}
