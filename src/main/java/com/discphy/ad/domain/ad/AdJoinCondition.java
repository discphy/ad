package com.discphy.ad.domain.ad;

import java.util.List;

public interface AdJoinCondition {

    AdJoinConditionType type();

    boolean isValid(String context);

    boolean isSatisfied(List<AdJoinedHistory> histories, String context);
}
