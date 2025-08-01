package com.discphy.ad.application.ad;

import com.discphy.ad.domain.ad.*;
import com.discphy.ad.domain.user.User;
import com.discphy.ad.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdFacade {

    public static final int GET_JOINABLE_MAX_SIZE = 10;

    private final AdService adService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AdInfo.Create create(AdCommand.Create command) {
        Ad ad = adService.create(command);
        return AdInfo.Create.from(ad);
    }

    @Transactional
    public AdInfo.Join join(AdCommand.Join command) {
        User user = userService.get(command.userId());
        AdJoinedHistory joinedUser = adService.join(command.adId(), user);

        eventPublisher.publishEvent(AdEvent.Joined.of(joinedUser));
        return AdInfo.Join.from(joinedUser);
    }

    @Transactional(readOnly = true)
    public AdInfo.GetJoinAble getJoinable(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        List<AdInfo.GetJoinAbleItem> items = adService.getJoinable(userId, now).stream()
            .limit(GET_JOINABLE_MAX_SIZE)
            .map(AdInfo.GetJoinAbleItem::from)
            .toList();

        return AdInfo.GetJoinAble.from(items);
    }

    @Transactional(readOnly = true)
    public AdInfo.GetJoinedHistories getJoinedHistories(AdCommand.JoinedHistoriesQuery command) {
        List<AdInfo.JoinedHistory> histories = adService.getJoinedHistories(command)
            .stream()
            .map(AdInfo.JoinedHistory::from)
            .toList();

        return AdInfo.GetJoinedHistories.of(command.userId(), command.page(), command.size(), histories);
    }
}
