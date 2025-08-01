package com.discphy.ad.domain.ad.condition;

import com.discphy.ad.common.DataSerializer;
import com.discphy.ad.domain.ad.AdJoinConditionType;
import com.discphy.ad.domain.ad.AdJoinCondition;
import com.discphy.ad.domain.ad.AdJoinedHistory;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class SpecificAdIdJoinCondition implements AdJoinCondition {

    @Override
    public AdJoinConditionType type() {
        return AdJoinConditionType.SPECIFIC_AD_ID;
    }

    @Override
    public boolean isValid(String context) {
        return deserialize(context)
            .map(SpecificAdId::validate)
            .orElse(false);
    }

    @Override
    public boolean isSatisfied(List<AdJoinedHistory> histories, String context) {
        return deserialize(context)
            .filter(SpecificAdId::validate)
            .map(c -> support(histories, c))
            .orElseThrow(() -> new CoreException(ErrorType.INTERNAL_ERROR, "광고 참여 조건이 올바르지 않습니다."));
    }

    private Optional<SpecificAdId> deserialize(String context) {
        return Optional.ofNullable(DataSerializer.deserialize(context, SpecificAdId.class));
    }

    private boolean support(List<AdJoinedHistory> histories, SpecificAdId condition) {
        return histories.stream()
            .anyMatch(h -> h.equalsAd(condition.adId));
    }

    record SpecificAdId(
        Long adId
    ) {
        public boolean validate() {
            return adId != null;
        }
    }
}