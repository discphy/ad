package com.discphy.ad.application.point;

import com.discphy.ad.domain.ad.AdEvent;
import com.discphy.ad.domain.point.PointClient;
import com.discphy.ad.domain.point.PointCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointEventListenerUnitTest {

    @InjectMocks
    private PointEventListener pointEventListener;

    @Mock
    private PointClient pointClient;

    @DisplayName("광고 참여 이벤트 수신 시, ")
    @Nested
    class Joined {

        @DisplayName("포인트 지급 요청을 한다.")
        @Test
        void handle() {
            // given
            AdEvent.Joined event = new AdEvent.Joined(
                1L,
                1L,
                100L,
                LocalDateTime.now()
            );

            // when
            pointEventListener.handle(event);

            // then
            verify(pointClient).reward(any(PointCommand.Reward.class));
        }
    }
}