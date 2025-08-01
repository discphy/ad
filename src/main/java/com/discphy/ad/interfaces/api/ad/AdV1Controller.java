package com.discphy.ad.interfaces.api.ad;

import com.discphy.ad.application.ad.AdFacade;
import com.discphy.ad.domain.ad.AdCommand;
import com.discphy.ad.domain.ad.AdInfo;
import com.discphy.ad.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdV1Controller {

    private final AdFacade adFacade;

    @PostMapping("/api/v1/ads")
    public ApiResponse<AdV1Dto.CreateResponse> create(@Valid @RequestBody AdV1Dto.CreateRequest request) {
        AdInfo.Create info = adFacade.create(request.toCommand());
        return ApiResponse.success(AdV1Dto.CreateResponse.from(info));
    }

    @PostMapping("/api/v1/ads/{adId}/join")
    public ApiResponse<AdV1Dto.JoinResponse> join(
        @PathVariable(name = "adId") Long adId,
        @RequestHeader("X-USER-ID") Long userId) {
        AdInfo.Join info = adFacade.join(AdCommand.Join.of(adId, userId));
        return ApiResponse.success(AdV1Dto.JoinResponse.from(info));
    }

    @GetMapping("/api/v1/ads")
    public ApiResponse<AdV1Dto.GetJoinAbleResponse> getJoinable(@RequestHeader("X-USER-ID") Long userId) {
        AdInfo.GetJoinAble info = adFacade.getJoinable(userId);
        return ApiResponse.success(AdV1Dto.GetJoinAbleResponse.from(info));
    }

    @GetMapping("/api/v1/ads/histories")
    public ApiResponse<AdV1Dto.GetJoinedHistoriesResponse> getJoinedHistories(
        @RequestHeader("X-USER-ID") Long userId,
        @RequestParam(value = "page", defaultValue = "1", required = false) int page,
        @RequestParam(value = "size", defaultValue = "20", required = false) int size) {
        AdCommand.JoinedHistoriesQuery command = AdCommand.JoinedHistoriesQuery.of(userId, page, size);
        AdInfo.GetJoinedHistories info = adFacade.getJoinedHistories(command);
        return ApiResponse.success(AdV1Dto.GetJoinedHistoriesResponse.from(info));
    }

}
