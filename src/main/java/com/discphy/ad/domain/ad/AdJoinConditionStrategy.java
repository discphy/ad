package com.discphy.ad.domain.ad;

import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AdJoinConditionStrategy {

    private final Map<AdJoinConditionType, AdJoinCondition> strategies;

    public AdJoinConditionStrategy(List<AdJoinCondition> strategies) {
        this.strategies = strategies.stream()
            .collect(Collectors.toMap(AdJoinCondition::type, strategy -> strategy));
    }

    public boolean isSatisfied(Ad ad, List<AdJoinedHistory> histories) {
        return Optional.ofNullable(strategies.get(ad.getType()))
            .map(strategy -> strategy.isSatisfied(histories, ad.getContext()))
            .orElseThrow(() -> new CoreException(ErrorType.INTERNAL_ERROR, "지원하지 않는 광고 타입입니다: " + ad.getType()));
    }

    public boolean isInvalid(AdJoinConditionType type, String context) {
        return Optional.ofNullable(strategies.get(type))
            .map(strategy -> !strategy.isValid(context))
            .orElse(true);
    }
}
