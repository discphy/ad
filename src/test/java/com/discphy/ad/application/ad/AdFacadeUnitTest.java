package com.discphy.ad.application.ad;

import com.discphy.ad.domain.ad.*;
import com.discphy.ad.domain.user.User;
import com.discphy.ad.domain.user.UserService;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdFacadeUnitTest {

    @InjectMocks
    private AdFacade adFacade;

    @Mock
    private AdService adService;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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

            Ad ad = Ad.create(command);

            when(adService.create(command))
                .thenReturn(ad);

            // when
            AdInfo.Create info = adFacade.create(command);

            // then
            assertThat(info.name()).isEqualTo(ad.getName());
            assertThat(info.rewardAmount()).isEqualTo(ad.getRewardAmount());
            assertThat(info.description()).isEqualTo(ad.getDescription());
            assertThat(info.imageUrl()).isEqualTo(ad.getImageUrl());
        }
    }

    @DisplayName("광고 참여 시, ")
    @Nested
    class Join {

        @DisplayName("사용자가 존재하지 않으면 예외를 던진다.")
        @Test
        void notExistUser() {
            // given
            Long adId = 1L;
            Long userId = 1L;
            AdCommand.Join command = new AdCommand.Join(adId, userId);

            when(userService.get(userId))
                .thenThrow(new CoreException(ErrorType.NOT_FOUND, "사용자가 존재하지 않습니다."));

            // when & then
            assertThatThrownBy(() -> adFacade.join(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("사용자가 존재하지 않습니다.");
        }


        @DisplayName("참여 완료 이벤트를 발행한다.")
        @Test
        void afterPublishJoinedEvent() {
            // given
            Long adId = 1L;
            Long userId = 1L;
            AdCommand.Join command = new AdCommand.Join(adId, userId);

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = User.builder()
                .id(userId)
                .name("사용자명")
                .build();

            AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now());

            when(userService.get(userId))
                .thenReturn(user);

            when(adService.join(adId, user))
                .thenReturn(joinedUser);

            // when
            adFacade.join(command);

            // then
            verify(eventPublisher).publishEvent(any(AdEvent.Joined.class));
        }

        @DisplayName("참여 정보를 반환한다.")
        @Test
        void returnJoinedInfo() {
            // given
            Long adId = 1L;
            Long userId = 1L;
            AdCommand.Join command = new AdCommand.Join(adId, userId);

            Ad ad = Ad.builder()
                .id(adId)
                .name("광고명")
                .rewardAmount(1_000L)
                .joinCount(1)
                .description("광고 설명")
                .imageUrl("https://example.com/image.jpg")
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
            User user = User.builder()
                .id(userId)
                .name("사용자명")
                .build();

            AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now());

            when(userService.get(userId))
                .thenReturn(user);

            when(adService.join(adId, user))
                .thenReturn(joinedUser);

            // when
            AdInfo.Join info = adFacade.join(command);

            // then
            assertThat(info.adId()).isEqualTo(adId);
            assertThat(info.userId()).isEqualTo(userId);
        }
    }

    @DisplayName("참여 가능한 광고 조회 시, ")
    @Nested
    class GetJoinable {

        @DisplayName("최대 10개의 광고를 조회한다.")
        @Test
        void withMaxLimit() {
            // given
            User user = User.builder()
                .id(1L)
                .name("사용자명")
                .build();
            when(adService.getJoinable(anyLong(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                    createAd("광고1"),
                    createAd("광고2"),
                    createAd("광고3"),
                    createAd("광고4"),
                    createAd("광고5"),
                    createAd("광고6"),
                    createAd("광고7"),
                    createAd("광고8"),
                    createAd("광고9"),
                    createAd("광고10"),
                    createAd("광고11") // 11번째 광고는 무시됨
                ));

            // when
            AdInfo.GetJoinAble info = adFacade.getJoinable(user.getId());

            // then
            assertThat(info.items()).hasSize(10);
        }

        private Ad createAd(String name) {
            AdCommand.Create command = new AdCommand.Create(
                name,
                1000L,
                10,
                "광고 설명1",
                "https://example.com/image1.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            return Ad.create(command);
        }
    }

    @DisplayName("광고 참여 이력 조회 시, ")
    @Nested
    class GetJoinedHistories {

        @DisplayName("참여 이력을 조회한다.")
        @Test
        void getJoinedHistories() {
            // given
            Long userId = 1L;
            int page = 1;
            int size = 10;
            AdCommand.JoinedHistoriesQuery command = new AdCommand.JoinedHistoriesQuery(userId, page, size);

            User user = User.builder()
                .id(userId)
                .name("사용자명")
                .build();

            List<AdJoinedHistory> histories = List.of(
                AdJoinedHistory.create(createAd("광고1"), user, LocalDateTime.now()),
                AdJoinedHistory.create(createAd("광고2"), user, LocalDateTime.now())
            );

            when(adService.getJoinedHistories(command))
                .thenReturn(histories);

            // when
            AdInfo.GetJoinedHistories info = adFacade.getJoinedHistories(command);

            // then
            assertThat(info.userId()).isEqualTo(userId);
            assertThat(info.page()).isEqualTo(page);
            assertThat(info.size()).isEqualTo(size);
            assertThat(info.histories()).hasSize(2);
        }

        private Ad createAd(String name) {
            return Ad.builder()
                .id(1L)
                .name(name)
                .rewardAmount(1000L)
                .joinCount(10)
                .description("광고 설명1")
                .imageUrl("https://example.com/image1.jpg")
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
        }
    }
}