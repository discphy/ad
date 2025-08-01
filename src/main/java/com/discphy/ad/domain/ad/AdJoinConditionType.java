package com.discphy.ad.domain.ad;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AdJoinConditionType {

    FIRST_JOIN("첫 참여"),
    COUNT_OVER("횟수 이상"),
    SPECIFIC_AD_ID("특정 광고 ID"),
    ;

    private final String description;
}
