package com.discphy.ad.domain.ad;

import com.discphy.ad.domain.ad.condition.CountOverJoinCondition;
import com.discphy.ad.domain.ad.condition.FirstJoinCondition;
import com.discphy.ad.domain.ad.condition.SpecificAdIdJoinCondition;
import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdJoinConditionTest {

    @DisplayName("첫 참여 조건 시, ")
    @Nested
    class FirstJoin {

        private AdJoinCondition conditionStrategy;

        @BeforeEach
        void setUp() {
            conditionStrategy = new FirstJoinCondition();
        }

        @DisplayName("첫 참여이면 참여 가능하다.")
        @Test
        void satisfied() {
            // given
            List<AdJoinedHistory> histories = List.of();

            // when
            boolean result = conditionStrategy.isSatisfied(histories, null);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("첫 참여가 아니면 참여 불가능하다.")
        @Test
        void notSatisfied() {
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

            List<AdJoinedHistory> histories = List.of(AdJoinedHistory.create(ad, user, LocalDateTime.now()));

            // when
            boolean result = conditionStrategy.isSatisfied(histories, null);

            // then
            assertThat(result).isFalse();
        }
    }

    @DisplayName("참여 횟수 조건 시, ")
    @Nested
    class CountOver {

        private AdJoinCondition conditionStrategy;

        @BeforeEach
        void setUp() {
            conditionStrategy = new CountOverJoinCondition();
        }

        @DisplayName("컨텍스트를 검증한다.")
        @Test
        void validateContext() {
            // given
            String context = """
                {
                    "joinCount": 0
                }
                """;

            // when & then
            assertThat(conditionStrategy.isValid(context)).isFalse();
        }

        @DisplayName("컨텍스트가 잘못되면 예외가 발생한다.")
        @Test
        void invalidContext() {
            // given
            List<AdJoinedHistory> histories = List.of();

            String context = """
                {
                    "joinCount": "invalid"
                }
                """;

            // when & then
            assertThatThrownBy(() -> conditionStrategy.isSatisfied(histories, context))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건이 올바르지 않습니다.");
        }

        @DisplayName("횟수를 만족하면 참여 가능하다.")
        @Test
        void satisfied() {
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

            List<AdJoinedHistory> histories = List.of(AdJoinedHistory.create(ad, user, LocalDateTime.now()));

            String context = """
                {
                    "joinCount": 1
                }
                """;

            // when
            boolean result = conditionStrategy.isSatisfied(histories, context);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("횟수를 만족하지 않으면 참여 불가능하다.")
        @Test
        void notSatisfied() {
            // given
            List<AdJoinedHistory> histories = List.of();

            String context = """
                {
                    "joinCount": 1
                }
                """;

            // when
            boolean result = conditionStrategy.isSatisfied(histories, context);

            // then
            assertThat(result).isFalse();
        }
    }

    @DisplayName("특정 광고 ID 조건 시, ")
    @Nested
    class SpecificAdId {

        private AdJoinCondition conditionStrategy;

        @BeforeEach
        void setUp() {
            conditionStrategy = new SpecificAdIdJoinCondition();
        }

        @DisplayName("컨텍스트를 검증한다.")
        @Test
        void validateContext() {
            // given
            String context = """
                {
                    "adId": null
                }
                """;

            // when & then
            assertThat(conditionStrategy.isValid(context)).isFalse();
        }

        @DisplayName("컨텍스트가 잘못되면 예외가 발생한다.")
        @Test
        void invalidContext() {
            // given
            List<AdJoinedHistory> histories = List.of();

            String context = """
                {
                    "adId": "invalid"
                }
                """;

            // when & then
            assertThatThrownBy(() -> conditionStrategy.isSatisfied(histories, context))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건이 올바르지 않습니다.");
        }

        @DisplayName("특정 광고 ID가 일치하면 참여 가능하다.")
        @Test
        void satisfied() {
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

            List<AdJoinedHistory> histories = List.of(AdJoinedHistory.create(ad, user, LocalDateTime.now()));

            String context = """
                {
                    "adId": 1
                }
                """;

            // when
            boolean result = conditionStrategy.isSatisfied(histories, context);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("특정 광고 ID가 일치하지 않으면 참여 불가능하다.")
        @Test
        void notSatisfied() {
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

            List<AdJoinedHistory> histories = List.of(AdJoinedHistory.create(ad, user, LocalDateTime.now()));

            String context = """
                {
                    "adId": 2
                }
                """;

            // when
            boolean result = conditionStrategy.isSatisfied(histories, context);

            // then
            assertThat(result).isFalse();
        }
    }
}