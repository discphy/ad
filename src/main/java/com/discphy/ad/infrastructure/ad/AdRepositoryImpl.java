package com.discphy.ad.infrastructure.ad;

import com.discphy.ad.domain.ad.Ad;
import com.discphy.ad.domain.ad.AdCommand;
import com.discphy.ad.domain.ad.AdJoinedHistory;
import com.discphy.ad.domain.ad.AdRepository;
import com.discphy.ad.infrastructure.ad.jpa.AdJoinedUserJpaRepository;
import com.discphy.ad.infrastructure.ad.jpa.AdJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdRepositoryImpl implements AdRepository {

    private final AdJpaRepository adJpaRepository;
    private final AdJoinedUserJpaRepository adJoinedUserJpaRepository;

    @Override
    public boolean existsByName(String name) {
        return adJpaRepository.existsByName(name);
    }

    @Override
    public Ad save(Ad ad) {
        return adJpaRepository.save(ad);
    }

    @Override
    public List<Ad> findJoinableAds(LocalDateTime dateTime) {
        return adJpaRepository.findJoinableAds(dateTime);
    }

    @Override
    public Optional<Ad> findByIdWithLock(Long id) {
        return adJpaRepository.findByIdWithLock(id);
    }

    @Override
    public AdJoinedHistory saveJoinedUser(AdJoinedHistory joinedUser) {
        return adJoinedUserJpaRepository.save(joinedUser);
    }

    @Override
    public List<AdJoinedHistory> findJoinedHistories(AdCommand.JoinedHistoriesQuery command) {
        Pageable pageable = PageRequest.of(command.page(), command.size());
        return adJoinedUserJpaRepository.findByUserIdOrderByJoinedAtAsc(command.userId(), pageable);
    }

    @Override
    public List<AdJoinedHistory> findJoinedHistories(Long userId) {
        return adJoinedUserJpaRepository.findByUserId(userId);
    }
}
