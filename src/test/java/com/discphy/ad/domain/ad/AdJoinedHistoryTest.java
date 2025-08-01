package com.discphy.ad.domain.ad;

import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdJoinedHistoryTest {

    @DisplayName("광고 참여 이력 등록 시, ")
    @Nested
    class Create {

        @DisplayName("광고는 존재해야 한다.")
        @Test
        void createWithNotExistsAd() {
            // given
            Ad ad = null;
            User user = User.builder()
                .id(1L)
                .name("카카오페이")
                .build();

            // when & then
            assertThatThrownBy(() -> AdJoinedHistory.create(ad, user, LocalDateTime.now()))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고가 존재하지 않습니다.");
        }

        @DisplayName("사용자는 존재해야 한다.")
        @Test
        void createWithNotExistsUser() {
            // given
            Ad ad = Ad.builder()
                .id(1L)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = null;

            // when & then
            assertThatThrownBy(() -> AdJoinedHistory.create(ad, user, LocalDateTime.now()))
                .isInstanceOf(CoreException.class)
                .hasMessage("사용자가 존재하지 않습니다.");
        }

        @DisplayName("정상적으로 생성한다.")
        @Test
        void create() {
            // given
            Ad ad = Ad.builder()
                .id(1L)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = User.builder()
                .id(1L)
                .name("카카오페이")
                .build();

            // when
            AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now());

            // then
            assertThat(joinedUser.getAdId()).isEqualTo(ad.getId());
            assertThat(joinedUser.getUserId()).isEqualTo(user.getId());
        }
    }

    @DisplayName("광고 이력이 같은지 확인할 때, ")
    @Nested
    class Equals {

        @DisplayName("광고 ID와 사용자 ID가 같으면 같은 이력으로 판단한다.")
        @Test
        void equalsAd() {
            // given
            Ad ad = Ad.builder()
                .id(1L)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user1 = User.builder()
                .id(1L)
                .name("카카오페이")
                .build();
            User user2 = User.builder()
                .id(2L)
                .name("카카오뱅크")
                .build();

            AdJoinedHistory history1 = AdJoinedHistory.create(
                ad,
                user1,
                LocalDateTime.now()
            );
            AdJoinedHistory history2 = AdJoinedHistory.create(
                ad,
                user2,
                LocalDateTime.now()
            );

            // when
            boolean isEqual = history1.equalsAd(history2.getAdId());

            // when & then
            assertThat(isEqual).isTrue();
        }
    }
}