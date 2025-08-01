package com.discphy.ad.domain.ad;

import com.discphy.ad.exception.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdTest {

    @DisplayName("광고 등록 시, ")
    @Nested
    class Create {

        @DisplayName("광고명은 필수여야 한다.")
        @ParameterizedTest
        @ValueSource(strings = {
            "",
            " "
        })
        void nameShouldRequired(String name) {
            // given
            AdCommand.Create command = new AdCommand.Create(
                name,
                1_000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            assertThatThrownBy(() -> Ad.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고명은 필수입니다.");
        }

        @DisplayName("적립 액수는 유효해야한다.")
        @ParameterizedTest
        @ValueSource(longs = {
            1_000_001L,
            -1L
        })
        void rewardAmountShouldValid(Long rewardAmount) {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                rewardAmount,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            assertThatThrownBy(() -> Ad.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("적립 액수가 유효하지 않습니다.");
        }

        @DisplayName("참여 가능 회수는 유효해야한다.")
        @ParameterizedTest
        @ValueSource(ints = {
            101,
            0
        })
        void joinCountShouldValid(int joinCount) {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                1_000L,
                joinCount,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            assertThatThrownBy(() -> Ad.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("참여 가능 회수가 유효하지 않습니다.");
        }

        @DisplayName("노출 기간은 필수여야한다.")
        @Test
        void periodShouldRequired() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                1_000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                null,
                null,
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            assertThatThrownBy(() -> Ad.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("노출 기간은 필수입니다.");
        }

        @DisplayName("노출 기간은 유효해야한다.")
        @Test
        void periodShouldValid() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                1_000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            assertThatThrownBy(() -> Ad.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("노출 기간이 유효하지 않습니다.");
        }

        @DisplayName("광고를 정상적으로 생성한다.")
        @Test
        void create() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                1_000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when
            Ad ad = Ad.create(command);

            // then
            assertThat(ad).isNotNull();
        }
    }

    @DisplayName("광고 참여 시, ")
    @Nested
    class Join {

        @DisplayName("참여 가능 회수가 0인 경우, 참여할 수 없다.")
        @Test
        void joinWithZeroJoinCount() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                1_000L,
                1,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            ad.join(); // 남은 참여 가능 회수를 0으로 만듭니다.

            // when & then
            assertThatThrownBy(ad::join)
                .isInstanceOf(CoreException.class)
                .hasMessage("참여가 불가능합니다.");
        }

        @DisplayName("광고 참여 시, 참여 가능 회수를 감소시킨다.")
        @Test
        void join() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                1_000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );
            Ad ad = Ad.create(command);

            // when
            ad.join();

            // then
            assertThat(ad.getJoinCount()).isEqualTo(9);
        }
    }

}