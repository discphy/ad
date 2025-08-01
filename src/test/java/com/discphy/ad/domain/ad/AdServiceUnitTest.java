package com.discphy.ad.domain.ad;

import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdServiceUnitTest {

    @InjectMocks
    private AdService adService;

    @Mock
    private AdRepository adRepository;

    @Mock
    private AdJoinConditionStrategy adJoinConditionStrategy;

    @DisplayName("광고 등록 시, ")
    @Nested
    class Create {

        @DisplayName("광고명이 이미 존재하면 생성에 실패한다.")
        @Test
        void withExistingName() {
            // given
            String name = "이미 존재하는 광고명";

            when(adRepository.existsByName(name))
                .thenReturn(true);

            AdCommand.Create command = new AdCommand.Create(
                name,
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            assertThatThrownBy(() -> adService.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("이미 존재하는 광고명입니다.");
        }

        @DisplayName("광고 참여 조건이 유효하지 않으면 생성에 실패한다.")
        @Test
        void withJoinCondition() {
            // given
            String name = "광고명";
            AdCommand.Create command = new AdCommand.Create(
                name,
                1_000L,
                100,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 0
                }
                """
            );

            when(adRepository.existsByName(name))
                .thenReturn(false);

            when(adJoinConditionStrategy.isInvalid(command.type(), command.context()))
                .thenReturn(true);

            // when
            assertThatThrownBy(() -> adService.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건이 유효하지 않습니다.");
        }

        @DisplayName("광고를 생성 후 저장한다.")
        @Test
        void create() {
            // given
            String name = "광고명";
            AdCommand.Create command = new AdCommand.Create(
                name,
                1_000L,
                100,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            when(adRepository.existsByName(name))
                .thenReturn(false);

            Ad savedAd = Ad.builder()
                .id(1L)
                .name(command.name())
                .rewardAmount(command.rewardAmount())
                .joinCount(command.joinCount())
                .description(command.description())
                .imageUrl(command.imageUrl())
                .startedAt(command.startedAt())
                .endedAt(command.endedAt())
                .build();

            when(adRepository.save(any(Ad.class)))
                .thenReturn(savedAd);

            // when
            Ad result = adService.create(command);

            // then
            assertThat(result.getId()).isEqualTo(savedAd.getId());
            verify(adRepository).save(any(Ad.class));
        }
    }

    @DisplayName("광고 참여 시, ")
    @Nested
    class Join {

        @DisplayName("광고가 존재하지 않으면 미등록 광고 에러를 응답한다.")
        @Test
        void withNotExistAd() {
            // given
            Long adId = 1L;
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            when(adRepository.findByIdWithLock(adId))
                .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adService.join(adId, user))
                .isInstanceOf(CoreException.class)
                .hasMessage("존재하지 않는 광고입니다.");
        }

        @DisplayName("광고 참여 횟수가 없으면 참여 불가 에러를 응답한다.")
        @Test
        void withNotExistJoinCount() {
            // given
            Long adId = 1L;
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .type(AdJoinConditionType.FIRST_JOIN)
                .build();

            ad.join(); // 이후 참여 불가

            when(adRepository.findByIdWithLock(adId))
                .thenReturn(Optional.of(ad));

            when(adJoinConditionStrategy.isSatisfied(ad, List.of()))
                .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> adService.join(adId, user))
                .isInstanceOf(CoreException.class)
                .hasMessage("참여가 불가능합니다.");
        }

        @DisplayName("광고 첫 참여 조건이 있는 경우에만 참여가 가능하다.")
        @Test
        void withFirstJoinCondition() {
            // given
            Long adId = 1L;
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .type(AdJoinConditionType.FIRST_JOIN)
                .build();

            AdJoinedHistory history = AdJoinedHistory.create(ad, user, LocalDateTime.now());

            when(adRepository.findByIdWithLock(adId))
                .thenReturn(Optional.of(ad));

            when(adRepository.findJoinedHistories(user.getId()))
                .thenReturn(List.of(history));

            when(adJoinConditionStrategy.isSatisfied(ad, List.of(history)))
                .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> adService.join(adId, user))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건을 만족하지 않습니다.");
        }

        @DisplayName("광고 참여 횟수 조건이 있는 경우에만 참여가 가능하다.")
        @Test
        void withCountOverCondition() {
            // given
            Long adId = 1L;
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .type(AdJoinConditionType.FIRST_JOIN)
                .context("""
                    {
                        "joinCount": 2
                    }
                    """)
                .build();

            AdJoinedHistory history = AdJoinedHistory.create(ad, user, LocalDateTime.now());

            when(adRepository.findByIdWithLock(adId))
                .thenReturn(Optional.of(ad));

            when(adRepository.findJoinedHistories(user.getId()))
                .thenReturn(List.of(history));

            when(adJoinConditionStrategy.isSatisfied(ad, List.of(history)))
                .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> adService.join(adId, user))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건을 만족하지 않습니다.");
        }

        @DisplayName("광고 특정 광고 이력 조건이 있는 경우에만 참여가 가능하다.")
        @Test
        void withSpecificAdIdCondition() {
            // given
            Long adId = 1L;
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .type(AdJoinConditionType.SPECIFIC_AD_ID)
                .context("""
                    {
                        "adId": 2
                    }
                    """)
                .build();

            AdJoinedHistory history = AdJoinedHistory.create(ad, user, LocalDateTime.now());

            when(adRepository.findByIdWithLock(adId))
                .thenReturn(Optional.of(ad));

            when(adRepository.findJoinedHistories(user.getId()))
                .thenReturn(List.of(history));

            when(adJoinConditionStrategy.isSatisfied(ad, List.of(history)))
                .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> adService.join(adId, user))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건을 만족하지 않습니다.");
        }

        @DisplayName("광고 참여 후, 참여 이력을 저장한다.")
        @Test
        void afterJoinedUserSave() {
            // given
            Long adId = 1L;
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .type(AdJoinConditionType.FIRST_JOIN)
                .build();

            when(adRepository.findByIdWithLock(adId))
                .thenReturn(Optional.of(ad));

            when(adJoinConditionStrategy.isSatisfied(ad, List.of()))
                .thenReturn(true);

            // when
            adService.join(adId, user);

            // then
            verify(adRepository).saveJoinedUser(any(AdJoinedHistory.class));
        }
    }

    @DisplayName("광고 조회 시, ")
    @Nested
    class GetJoinable {

        @DisplayName("참여 가능한 광고만 조회한다.")
        @Test
        void onlyJoinable() {
            // given
            LocalDateTime now = LocalDateTime.now();
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();

            Ad ad1 = Ad.builder()
                .id(1L)
                .name("광고1")
                .rewardAmount(2_000L)
                .joinCount(100)
                .description("광고1 설명")
                .imageUrl("https://example.com/ad1.jpg")
                .startedAt(now.minusDays(1))
                .endedAt(now.plusDays(1))
                .type(AdJoinConditionType.FIRST_JOIN)
                .build();

            Ad ad2 = Ad.builder()
                .id(2L)
                .name("광고2")
                .rewardAmount(3_000L)
                .joinCount(100)
                .description("광고2 설명")
                .imageUrl("https://example.com/ad2.jpg")
                .startedAt(now.minusDays(1))
                .endedAt(now.plusDays(1))
                .type(AdJoinConditionType.COUNT_OVER)
                .context("""
                    {
                        "joinCount": 2
                    }
                """)
                .build();

            Ad ad3 = Ad.builder()
                .id(3L)
                .name("광고3")
                .rewardAmount(4_000L)
                .joinCount(100)
                .description("광고3 설명")
                .imageUrl("https://example.com/ad2.jpg")
                .startedAt(now.minusDays(1))
                .endedAt(now.plusDays(1))
                .type(AdJoinConditionType.SPECIFIC_AD_ID)
                .context("""
                    {
                        "adId": 4
                    }
                """)
                .build();

            when(adJoinConditionStrategy.isSatisfied(any(Ad.class), anyList()))
                .thenReturn(false);

            when(adRepository.findJoinableAds(now))
                .thenReturn(List.of(ad1, ad2, ad3));

            when(adRepository.findJoinedHistories(user.getId()))
                .thenReturn(List.of(
                    AdJoinedHistory.create(ad1, user, now.minusDays(2))
                ));

            // when
            List<Ad> joinableAds = adService.getJoinable(user.getId(), now);

            // then
            assertThat(joinableAds).isEmpty();
        }

        @DisplayName("적립 액수가 높은 순서로 정렬되어야 한다.")
        @Test
        void orderByRewardAmountDesc() {
            // given
            LocalDateTime now = LocalDateTime.now();
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();
            Ad ad1 = Ad.create(new AdCommand.Create(
                "광고1",
                3_000L,
                100,
                "광고1 설명",
                "https://example.com/ad1.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            Ad ad2 = Ad.create(new AdCommand.Create(
                "광고2",
                5_000L,
                100,
                "광고2 설명",
                "https://example.com/ad2.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));

            when(adRepository.findJoinableAds(now))
                .thenReturn(List.of(ad2, ad1));

            when(adRepository.findJoinedHistories(user.getId()))
                .thenReturn(List.of());

            when(adJoinConditionStrategy.isSatisfied(any(Ad.class), anyList()))
                .thenReturn(true);

            // when
            List<Ad> joinableAds = adService.getJoinable(user.getId(), now);

            // then
            assertThat(joinableAds).hasSize(2)
                .extracting(Ad::getName)
                .containsExactly("광고2", "광고1");
        }
    }

    @DisplayName("광고 이력 조회 시, ")
    @Nested
    class GetHistories {

        @DisplayName("광고 참여 시각이 오래된 순으로 정렬되어야 한다.")
        @Test
        void orderByJoinedAtAsc() {
            // given
            Long userId = 1L;
            Ad ad = Ad.builder()
                .id(1L)
                .name("광고명")
                .rewardAmount(2_000L)
                .joinCount(100)
                .description("새로운 광고 설명")
                .imageUrl("https://example.com/image2.jpg")
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = User.builder()
                .id(userId)
                .name("사용자명")
                .build();

            AdJoinedHistory joinedUser1 = AdJoinedHistory.create(ad, user, LocalDateTime.of(2025, 7, 26, 13, 0, 1));
            AdJoinedHistory joinedUser2 = AdJoinedHistory.create(ad, user, LocalDateTime.of(2025, 7, 26, 13, 0, 0));

            AdCommand.JoinedHistoriesQuery command = AdCommand.JoinedHistoriesQuery.of(userId, 1, 2);
            when(adRepository.findJoinedHistories(command))
                .thenReturn(List.of(joinedUser2, joinedUser1));

            // when
            List<AdJoinedHistory> joinedUsers = adService.getJoinedHistories(command);

            // then
            assertThat(joinedUsers).hasSize(2)
                .extracting(AdJoinedHistory::getJoinedAt)
                .containsExactly(
                    LocalDateTime.of(2025, 7, 26, 13, 0, 0),
                    LocalDateTime.of(2025, 7, 26, 13, 0, 1)
                );
        }

        @DisplayName("페이지네이션이 적용 되어야 한다.")
        @Test
        void pagination() {
            // given
            Long userId = 1L;
            Ad ad = Ad.builder()
                .id(1L)
                .name("광고명")
                .rewardAmount(2_000L)
                .joinCount(100)
                .description("새로운 광고 설명")
                .imageUrl("https://example.com/image2.jpg")
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = User.builder()
                .id(userId)
                .name("사용자명")
                .build();

            List<AdJoinedHistory> joinedHistories = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                joinedHistories.add(AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(i)));
            }

            when(adRepository.findJoinedHistories(AdCommand.JoinedHistoriesQuery.of(userId, 1, 5)))
                .thenReturn(joinedHistories.subList(0, 5));
            when(adRepository.findJoinedHistories(AdCommand.JoinedHistoriesQuery.of(userId, 2, 5)))
                .thenReturn(joinedHistories.subList(5, 10));

            // when
            List<AdJoinedHistory> joinedUsersPage1 = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(userId, 1, 5));
            List<AdJoinedHistory> joinedUsersPage2 = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(userId, 2, 5));

            // then
            assertThat(joinedUsersPage1).hasSize(5);
            assertThat(joinedUsersPage2).hasSize(5);
        }

        @DisplayName("최대 50개까지만 이력을 조회할 수 있다.")
        @Test
        void maxLimit() {
            // given
            Long userId = 1L;
            Ad ad = Ad.builder()
                .id(1L)
                .name("광고명")
                .rewardAmount(2_000L)
                .joinCount(100)
                .description("새로운 광고 설명")
                .imageUrl("https://example.com/image2.jpg")
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = User.builder()
                .id(userId)
                .name("사용자명")
                .build();

            List<AdJoinedHistory> joinedHistories = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                joinedHistories.add(AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(i)));
            }

            when(adRepository.findJoinedHistories(AdCommand.JoinedHistoriesQuery.of(userId, 1, 51)))
                .thenReturn(joinedHistories.subList(0, 50));

            // when
            List<AdJoinedHistory> joinedUsers = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(userId, 1, 51));

            // then
            assertThat(joinedUsers).hasSize(50);
        }
    }
}