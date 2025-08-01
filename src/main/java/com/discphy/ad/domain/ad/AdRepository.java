package com.discphy.ad.domain.ad;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdRepository {

    boolean existsByName(String name);

    Ad save(Ad ad);

    List<Ad> findJoinableAds(LocalDateTime dateTime);

    Optional<Ad> findByIdWithLock(Long id);

    AdJoinedHistory saveJoinedUser(AdJoinedHistory joinedUser);

    List<AdJoinedHistory> findJoinedHistories(AdCommand.JoinedHistoriesQuery command);

    List<AdJoinedHistory> findJoinedHistories(Long userId);
}
