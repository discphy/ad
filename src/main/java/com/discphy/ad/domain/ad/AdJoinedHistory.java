package com.discphy.ad.domain.ad;

import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "ad_joined_history",
    indexes = {
        @Index(name = "idx_ad_joined_history", columnList = "user_id, joined_at")
    }
)
public class AdJoinedHistory {

    @Id
    @Column(name = "ad_joined_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long adId;
    private String name;
    private long rewardAmount;
    private LocalDateTime joinedAt;

    private AdJoinedHistory(Ad ad, User user, LocalDateTime joinedAt) {
        validateAd(ad);
        validateUser(user);

        this.adId = ad.getId();
        this.name = ad.getName();
        this.rewardAmount = ad.getRewardAmount();
        this.userId = user.getId();
        this.joinedAt = joinedAt;
    }

    public static AdJoinedHistory create(Ad ad, User user, LocalDateTime dateTime) {
        return new AdJoinedHistory(ad, user, dateTime);
    }

    public boolean equalsAd(Long adId) {
        return this.adId.equals(adId);
    }

    private void validateAd(Ad ad) {
        if (ad == null || ad.getId() == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "광고가 존재하지 않습니다.");
        }
    }

    private void validateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자가 존재하지 않습니다.");
        }
    }
}
