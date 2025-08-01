package com.discphy.ad.domain.ad.condition;

import com.discphy.ad.domain.ad.AdJoinConditionType;
import com.discphy.ad.domain.ad.AdJoinConditionWithoutContext;
import com.discphy.ad.domain.ad.AdJoinedHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FirstJoinCondition implements AdJoinConditionWithoutContext {

    @Override
    public AdJoinConditionType type() {
        return AdJoinConditionType.FIRST_JOIN;
    }

    @Override
    public boolean isSatisfied(List<AdJoinedHistory> histories) {
        return histories.isEmpty();
    }
}