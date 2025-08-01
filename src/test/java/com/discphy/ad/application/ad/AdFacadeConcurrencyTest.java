package com.discphy.ad.application.ad;

import com.discphy.ad.domain.ad.Ad;
import com.discphy.ad.domain.ad.AdCommand;
import com.discphy.ad.domain.ad.AdJoinConditionType;
import com.discphy.ad.domain.ad.AdJoinedHistory;
import com.discphy.ad.domain.user.User;
import com.discphy.ad.infrastructure.ad.jpa.AdJoinedUserJpaRepository;
import com.discphy.ad.infrastructure.ad.jpa.AdJpaRepository;
import com.discphy.ad.infrastructure.user.jpa.UserJpaRepository;
import com.discphy.ad.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.discphy.ad.utils.ConcurrencyExecutor.executeConcurrency;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("test")
@SpringBootTest
class AdFacadeConcurrencyTest {

    @Autowired
    private AdFacade adFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private AdJpaRepository adJpaRepository;

    @Autowired
    private AdJoinedUserJpaRepository adJoinedUserJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("광고 참여 시, ")
    @Nested
    class Join {

        @DisplayName("하나의 광고에 동시에 여러 사용자가 참여할 수 있다.")
        @Test
        void canJoinAdConcurrently() {
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

            User user1 = User.create("사용자1");
            User user2 = User.create("사용자2");
            userJpaRepository.saveAll(List.of(user1, user2));

            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            // when
            executeConcurrency(List.of(
                () -> {
                    try {
                        adFacade.join(AdCommand.Join.of(ad.getId(), user1.getId()));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                },
                () -> {
                    try {
                        adFacade.join(AdCommand.Join.of(ad.getId(), user2.getId()));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                }
            ));

            // then
            List<AdJoinedHistory> histories = adJoinedUserJpaRepository.findAll();
            Ad updated = adJpaRepository.findById(ad.getId()).get();
            assertAll(
                () -> assertThat(successCount.get()).isEqualTo(2),
                () -> assertThat(failCount.get()).isEqualTo(0),
                () -> assertThat(histories).hasSize(2),
                () -> assertThat(updated.getJoinCount()).isEqualTo(8)
            );
        }

        @DisplayName("하나의 광고에 동시에 여러 사용자가 참여할 때, 참여 가능한 인원 수를 초과하면 예외가 발생한다.")
        @Test
        void cannotJoinAdExceedingMaxUsers() {
            // given
            Ad ad = Ad.create(new AdCommand.Create(
                "테스트 광고",
                1_000L,
                1, // 최대 참여 인원 1명
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.save(ad);

            User user1 = User.create("사용자1");
            User user2 = User.create("사용자2");
            userJpaRepository.saveAll(List.of(user1, user2));

            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            // when
            executeConcurrency(List.of(
                () -> {
                    try {
                        adFacade.join(AdCommand.Join.of(ad.getId(), user1.getId()));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                },
                () -> {
                    try {
                        adFacade.join(AdCommand.Join.of(ad.getId(), user2.getId()));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                }
            ));

            // then
            List<AdJoinedHistory> histories = adJoinedUserJpaRepository.findAll();
            Ad updated = adJpaRepository.findById(ad.getId()).get();
            assertAll(
                () -> assertThat(successCount.get()).isEqualTo(1),
                () -> assertThat(failCount.get()).isEqualTo(1),
                () -> assertThat(histories).hasSize(1),
                () -> assertThat(updated.getJoinCount()).isEqualTo(0)
            );
        }

    }
}