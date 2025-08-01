package com.discphy.ad.application.ad;

import com.discphy.ad.domain.ad.*;
import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.infrastructure.ad.jpa.AdJoinedUserJpaRepository;
import com.discphy.ad.infrastructure.ad.jpa.AdJpaRepository;
import com.discphy.ad.infrastructure.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@RecordApplicationEvents
@SpringBootTest
class AdFacadeIntegrationTest {

    @Autowired
    private AdFacade adFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private AdJpaRepository adJpaRepository;

    @Autowired
    private AdJoinedUserJpaRepository adJoinedUserJpaRepository;

    @Autowired
    protected ApplicationEvents events;

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

        @DisplayName("생성된 광고 정보를 반환한다.")
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
            AdInfo.Create info = adFacade.create(command);

            // then
            assertThat(info).isNotNull();
            assertThat(info.id()).isNotNull();
            assertThat(info.name()).isEqualTo(command.name());
            assertThat(info.rewardAmount()).isEqualTo(command.rewardAmount());
            assertThat(info.description()).isEqualTo(command.description());
            assertThat(info.imageUrl()).isEqualTo(command.imageUrl());
        }
    }

    @DisplayName("광고 참여 시, ")
    @Nested
    class Join {

        @DisplayName("사용자가 존재하지 않으면 예외를 던진다.")
        @Test
        void notExistUser() {
            // given
            Long notExistUserId = 999L;
            AdCommand.Join command = new AdCommand.Join(1L, notExistUserId);
            assertThat(userJpaRepository.findById(notExistUserId)).isNotPresent();

            // when & then
            assertThatThrownBy(() -> adFacade.join(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("사용자가 존재하지 않습니다.");
        }

        @DisplayName("참여 완료 이벤트를 발행한다.")
        @Test
        void afterPublishJoinedEvent() {
            // given
            Ad ad = Ad.create(new AdCommand.Create(
                "테스트 광고",
                1_000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.save(ad);

            AdCommand.Join command = new AdCommand.Join(ad.getId(), user.getId());

            // when
            AdInfo.Join info = adFacade.join(command);

            // then
            assertThat(info).isNotNull();
            assertThat(info.adId()).isEqualTo(ad.getId());
            assertThat(info.userId()).isEqualTo(user.getId());
            assertThat(events.stream(AdEvent.Joined.class).count()).isEqualTo(1);
        }
    }

    @DisplayName("참여 가능한 광고 조회 시, ")
    @Nested
    class GetJoinable {

        @DisplayName("최대 10개의 광고를 조회한다.")
        @Test
        void withMaxLimit() {
            // given
            List<Ad> list = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                Ad ad = Ad.create(new AdCommand.Create(
                    "광고 " + i,
                    1_000L + i,
                    10 + i,
                    "광고 설명 " + i,
                    "https://example.com/image" + i + ".jpg",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    AdJoinConditionType.FIRST_JOIN,
                    null
                ));
                list.add(ad);
            }
            adJpaRepository.saveAll(list);

            // when
            AdInfo.GetJoinAble info = adFacade.getJoinable(user.getId());

            // then
            assertThat(info.items()).hasSize(10)
                .extracting(AdInfo.GetJoinAbleItem::name)
                .containsExactly(
                    "광고 10",
                    "광고 9",
                    "광고 8",
                    "광고 7",
                    "광고 6",
                    "광고 5",
                    "광고 4",
                    "광고 3",
                    "광고 2",
                    "광고 1"
                );
        }

        @DisplayName("광고 참여 이력 조회 시, ")
        @Nested
        class GetJoinedHistories {

            @DisplayName("참여 이력을 조회한다.")
            @Test
            void getJoinedHistories() {
                // given
                int page = 1;
                int size = 10;
                Ad ad = Ad.create(new AdCommand.Create(
                    "테스트 광고",
                    1_000L,
                    10,
                    "광고 설명",
                    "https://example.com/image.jpg",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    AdJoinConditionType.FIRST_JOIN,
                    null
                ));
                adJpaRepository.save(ad);
                adJoinedUserJpaRepository.save(AdJoinedHistory.create(ad, user, LocalDateTime.now()));

                // when
                AdInfo.GetJoinedHistories info = adFacade.getJoinedHistories(new AdCommand.JoinedHistoriesQuery(user.getId(), 0, 10));

                // then
                assertThat(info.userId()).isEqualTo(user.getId());
                assertThat(info.page()).isEqualTo(page);
                assertThat(info.size()).isEqualTo(size);
                assertThat(info.histories()).hasSize(1)
                    .extracting("adId", "userId")
                    .containsExactly(tuple(ad.getId(), user.getId()));
            }
        }
    }
}