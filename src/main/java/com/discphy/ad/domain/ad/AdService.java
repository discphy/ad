package com.discphy.ad.domain.ad;

import com.discphy.ad.domain.user.User;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdService {

    private final AdRepository adRepository;
    private final AdJoinConditionStrategy adJoinConditionStrategy;

    @Transactional
    public Ad create(AdCommand.Create command) {
        if (adRepository.existsByName(command.name())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 광고명입니다.");
        }

        if (adJoinConditionStrategy.isInvalid(command.type(), command.context())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "광고 참여 조건이 유효하지 않습니다.");
        }

        Ad ad = Ad.create(command);
        return adRepository.save(ad);
    }

    @Transactional
    public AdJoinedHistory join(Long adId, User user) {
        Ad ad = adRepository.findByIdWithLock(adId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 광고입니다."));

        List<AdJoinedHistory> histories = adRepository.findJoinedHistories(user.getId());
        if (!adJoinConditionStrategy.isSatisfied(ad, histories)) {
            throw new CoreException(ErrorType.CONFLICT, "광고 참여 조건을 만족하지 않습니다.");
        }

        ad.join();

        AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now());
        return adRepository.saveJoinedUser(joinedUser);
    }

    @Transactional(readOnly = true)
    public List<Ad> getJoinable(Long userId, LocalDateTime dateTime) {
        List<AdJoinedHistory> histories = adRepository.findJoinedHistories(userId);

        return adRepository.findJoinableAds(dateTime).stream()
            .filter(ad -> adJoinConditionStrategy.isSatisfied(ad, histories))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdJoinedHistory> getJoinedHistories(AdCommand.JoinedHistoriesQuery command) {
        return adRepository.findJoinedHistories(command);
    }
}
