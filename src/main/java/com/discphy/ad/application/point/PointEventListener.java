package com.discphy.ad.application.point;

import com.discphy.ad.domain.ad.AdEvent;
import com.discphy.ad.domain.point.PointClient;
import com.discphy.ad.domain.point.PointCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final PointClient pointClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AdEvent.Joined event) {
        pointClient.reward(PointCommand.Reward.of(event.userId(), event.rewardAmount()));
    }
}
