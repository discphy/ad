package com.discphy.ad.domain.point;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PointClient {

    public void reward(PointCommand.Reward command) {
        log.info("포인트 적립 API 호출 - userId: {}, amount: {}", command.userId(), command.amount());
        try {
            Thread.sleep(300); // 포인트 API 응답 지연 시뮬레이션
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("포인트 적립 API 응답 - userId: {}, amount: {}", command.userId(), command.amount());
    }
}
