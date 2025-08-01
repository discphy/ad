package com.discphy.ad.domain.ad;

import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.infrastructure.ad.jpa.AdJoinedUserJpaRepository;
import com.discphy.ad.infrastructure.ad.jpa.AdJpaRepository;
import com.discphy.ad.infrastructure.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class AdServiceIntegrationTest {

    @Autowired
    private AdService adService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private AdJpaRepository adJpaRepository;

    @Autowired
    private AdJoinedUserJpaRepository adJoinedUserJpaRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create("테스트 사용자");
        userJpaRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        adJpaRepository.deleteAllInBatch();
        userJpaRepository.deleteAllInBatch();
        adJoinedUserJpaRepository.deleteAllInBatch();
    }

    @DisplayName("광고 생성 시, ")
    @Nested
    class Create {

        @DisplayName("광고명이 이미 존재하면 생성에 실패한다.")
        @Test
        void withExistingName() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "중복 광고명",
                1_000L,
                100,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );
            adJpaRepository.save(Ad.create(command));

            // when & then
            assertThatThrownBy(() -> adService.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("이미 존재하는 광고명입니다.");
        }

        @DisplayName("광고 참여 조건이 유효하지 않으면 생성에 실패한다.")
        @Test
        void withJoinCondition() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "유효하지 않은 참여 조건 광고",
                1_000L,
                100,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null,
                null
            );

            // when & then
            assertThatThrownBy(() -> adService.create(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건이 유효하지 않습니다.");
        }

        @DisplayName("광고를 생성 후 저장한다.")
        @Test
        void create() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "새로운 광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when
            Ad ad = adService.create(command);
            Ad createdAd = adJpaRepository.findById(ad.getId()).get();

            // then
            assertThat(createdAd.getId()).isEqualTo(ad.getId());
            assertThat(createdAd.getName()).isEqualTo(ad.getName());
            assertThat(createdAd.getRewardAmount()).isEqualTo(command.rewardAmount());
            assertThat(createdAd.getJoinCount()).isEqualTo(command.joinCount());
            assertThat(createdAd.getDescription()).isEqualTo(command.description());
            assertThat(createdAd.getImageUrl()).isEqualTo(command.imageUrl());
        }
    }

    @DisplayName("광고 참여 시, ")
    @Nested
    class Join {

        @DisplayName("광고가 존재하지 않으면 미등록 광고 에러를 응답한다.")
        @Test
        void withNotExistAd() {
            // given
            Long notExistAdId = 999L;
            assertThat(adJpaRepository.findById(notExistAdId)).isNotPresent();

            // when & then
            assertThatThrownBy(() -> adService.join(notExistAdId, user))
                .isInstanceOf(CoreException.class)
                .hasMessage("존재하지 않는 광고입니다.");
        }

        @DisplayName("광고 참여 횟수가 없으면 참여 불가 에러를 응답한다.")
        @Test
        void withNotExistJoinCount() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad ad = Ad.create(new AdCommand.Create(
                "참여 횟수 없는 광고",
                1_000L,
                1,
                "참여 횟수가 없는 광고 설명",
                "https://example.com/no_join_count.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            ad.join(); // 이후 참여 불가
            adJpaRepository.save(ad);

            // when & then
            assertThatThrownBy(() -> adService.join(ad.getId(), user))
                .isInstanceOf(CoreException.class)
                .hasMessage("참여가 불가능합니다.");
        }

        @DisplayName("광고 첫 참여 조건이 있는 경우에만 참여가 가능하다.")
        @Test
        void withFirstJoinCondition() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad ad = Ad.create(new AdCommand.Create(
                "참여 가능한 광고",
                2_000L,
                100,
                "참여 가능한 광고 설명",
                "https://example.com/joinable.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.save(ad);

            AdJoinedHistory history = AdJoinedHistory.create(ad, user, LocalDateTime.now());
            adJoinedUserJpaRepository.save(history);

            // when & then
            assertThatThrownBy(() -> adService.join(ad.getId(), user))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건을 만족하지 않습니다.");
        }

        @DisplayName("광고 참여 횟수 조건이 있는 경우에만 참여가 가능하다.")
        @Test
        void withCountOverCondition() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad ad = Ad.create(new AdCommand.Create(
                "참여 가능한 광고",
                2_000L,
                100,
                "참여 가능한 광고 설명",
                "https://example.com/joinable.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 2
                }
                """
            ));
            adJpaRepository.save(ad);

            AdJoinedHistory history = AdJoinedHistory.create(ad, user, LocalDateTime.now());
            adJoinedUserJpaRepository.save(history);

            // when & then
            assertThatThrownBy(() -> adService.join(ad.getId(), user))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건을 만족하지 않습니다.");
        }

        @DisplayName("광고 특정 광고 이력 조건이 있는 경우에만 참여가 가능하다.")
        @Test
        void withSpecificAdIdCondition() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad ad = Ad.create(new AdCommand.Create(
                "참여 가능한 광고",
                2_000L,
                100,
                "참여 가능한 광고 설명",
                "https://example.com/joinable.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.SPECIFIC_AD_ID,
                """
                {
                    "adId": 12345
                }
                """
            ));
            adJpaRepository.save(ad);

            // when & then
            assertThatThrownBy(() -> adService.join(ad.getId(), user))
                .isInstanceOf(CoreException.class)
                .hasMessage("광고 참여 조건을 만족하지 않습니다.");
        }

        @DisplayName("광고 참여 후, 참여 이력을 저장한다.")
        @Test
        void afterJoinedUserSave() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad ad = Ad.create(new AdCommand.Create(
                "참여 가능한 광고",
                2_000L,
                100,
                "참여 가능한 광고 설명",
                "https://example.com/joinable.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.save(ad);

            // when
            AdJoinedHistory joinedHistory = adService.join(ad.getId(), user);
            Ad updatedAd = adJpaRepository.findById(ad.getId()).get();
            AdJoinedHistory savedJoinedHistory = adJoinedUserJpaRepository.findById(joinedHistory.getId()).get();

            // then
            assertThat(updatedAd.getJoinCount()).isEqualTo(99); // 참여 횟수가 감소해야 함
            assertThat(savedJoinedHistory.getAdId()).isEqualTo(ad.getId());
            assertThat(savedJoinedHistory.getUserId()).isEqualTo(user.getId());
            assertThat(savedJoinedHistory.getJoinedAt()).isNotNull();
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
            Ad joinableAd = Ad.create(new AdCommand.Create(
                "참여 가능한 광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            Ad countOverConditionAd = Ad.create(new AdCommand.Create(
                "참여 횟수 조건 광고",
                1_500L,
                50,
                "참여 횟수 조건 광고 설명",
                "https://example.com/count_over.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 2
                }
                """
            ));
            Ad specificAdIdConditionAd = Ad.create(new AdCommand.Create(
                "특정 광고 이력 조건 광고",
                2_500L,
                30,
                "특정 광고 이력 조건 광고 설명",
                "https://example.com/specific_ad.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.SPECIFIC_AD_ID,
                """
                {
                    "adId": 12345
                }
                """
            ));
            Ad notDisplayedAd = Ad.create(new AdCommand.Create(
                "노출되지 않는 광고",
                1_000L,
                50,
                "노출되지 않는 광고 설명",
                "https://example.com/not_displayed.jpg",
                now.minusDays(2),
                now.minusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            Ad notExistJoinCountAd = Ad.create(new AdCommand.Create(
                "참여 불가능한 광고",
                3_000L,
                1,
                "참여 불가능한 광고 설명",
                "https://example.com/not_joinable.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            notExistJoinCountAd.join(); // 참여 카운트가 0이 되도록 설정

            adJpaRepository.saveAll(List.of(
                joinableAd, countOverConditionAd, specificAdIdConditionAd, notDisplayedAd, notExistJoinCountAd
            ));

            // when
            List<Ad> joinableAds = adService.getJoinable(user.getId(), now);

            // then
            assertThat(joinableAds).hasSize(1)
                .extracting(Ad::getName)
                .containsExactly("참여 가능한 광고명");
        }

        @DisplayName("적립 액수가 높은 순서로 정렬되어야 한다.")
        @Test
        void orderByRewardAmountDesc() {
            // given
            LocalDateTime now = LocalDateTime.now();
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
            adJpaRepository.saveAll(List.of(ad1, ad2));

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
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            adJpaRepository.save(ad);

            AdJoinedHistory joinedUser1 = AdJoinedHistory.create(ad, user, LocalDateTime.of(2025, 7, 26, 13, 0, 1));
            AdJoinedHistory joinedUser2 = AdJoinedHistory.create(ad, user, LocalDateTime.of(2025, 7, 26, 13, 0, 0));
            adJoinedUserJpaRepository.saveAll(List.of(joinedUser1, joinedUser2));

            // when
            List<AdJoinedHistory> joinedUsers = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(user.getId(), 1, 2));

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
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            adJpaRepository.save(ad);

            for (int i = 0; i < 10; i++) {
                AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(i));
                adJoinedUserJpaRepository.save(joinedUser);
            }

            // when
            List<AdJoinedHistory> joinedUsersPage1 = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(user.getId(), 1, 5));
            List<AdJoinedHistory> joinedUsersPage2 = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(user.getId(), 2, 5));

            // then
            assertThat(joinedUsersPage1).hasSize(5);
            assertThat(joinedUsersPage2).hasSize(5);
        }

        @DisplayName("최대 50개까지만 이력을 조회할 수 있다.")
        @Test
        void maxLimit() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            adJpaRepository.save(ad);

            for (int i = 0; i < 50; i++) {
                AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(i));
                adJoinedUserJpaRepository.save(joinedUser);
            }

            // when
            List<AdJoinedHistory> joinedUsers = adService.getJoinedHistories(AdCommand.JoinedHistoriesQuery.of(user.getId(), 1, 51));

            // then
            assertThat(joinedUsers).hasSize(50);
        }
    }

}