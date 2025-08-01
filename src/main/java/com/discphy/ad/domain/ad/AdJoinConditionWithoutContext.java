package com.discphy.ad.domain.ad;

import java.util.List;

public interface AdJoinConditionWithoutContext extends AdJoinCondition {

    boolean isSatisfied(List<AdJoinedHistory> histories);

    @Override
    default boolean isSatisfied(List<AdJoinedHistory> histories, String context) {
        return isSatisfied(histories);
    }

    @Override
    default boolean isValid(String context) {
        return true;
    }
}
