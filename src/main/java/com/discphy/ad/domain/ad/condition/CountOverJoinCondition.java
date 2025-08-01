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
public class CountOverJoinCondition implements AdJoinCondition {

    @Override
    public AdJoinConditionType type() {
        return AdJoinConditionType.COUNT_OVER;
    }

    @Override
    public boolean isValid(String context) {
        return deserialize(context)
            .map(CountOver::validate)
            .orElse(false);
    }

    @Override
    public boolean isSatisfied(List<AdJoinedHistory> histories, String context) {
        return deserialize(context)
            .filter(CountOver::validate)
            .map(c -> support(histories, c))
            .orElseThrow(() -> new CoreException(ErrorType.INTERNAL_ERROR, "광고 참여 조건이 올바르지 않습니다."));
    }

    private Optional<CountOver> deserialize(String context) {
        return Optional.ofNullable(DataSerializer.deserialize(context, CountOver.class));
    }

    private boolean support(List<AdJoinedHistory> histories, CountOver condition) {
        return histories.size() >= condition.joinCount;
    }

    record CountOver(
        int joinCount
    ) {
        public boolean validate() {
            return joinCount > 0;
        }
    }
}