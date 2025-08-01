package com.discphy.ad.interfaces.api.ad;

import com.discphy.ad.domain.ad.AdCommand;
import com.discphy.ad.domain.ad.AdInfo;
import com.discphy.ad.domain.ad.AdJoinConditionType;
import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AdV1Dto {

    public record CreateRequest(
        @NotBlank(message = "광고명은 비어있을 수 없습니다.") String name,
        @NotNull(message = "광고 참여 적립 액수는 필수입니다.") Long rewardAmount,
        @NotNull(message = "광고 참여 가능 횟수는 필수입니다.") Integer joinCount,
        String description,
        String imageUrl,
        @NotNull(message = "광고 노출 시작 시간은 필수입니다.") String startedAt,
        @NotNull(message = "광고 노출 종료 시간은 필수입니다.") String endedAt,
        @NotNull(message = "광고 참여 조건은 필수입니다.") AdJoinConditionType type,
        String context
    ) {
        public AdCommand.Create toCommand() {
            return new AdCommand.Create(
                name,
                rewardAmount,
                joinCount,
                description,
                imageUrl,
                dateParse(startedAt, LocalTime.of(0, 0, 0)),
                dateParse(endedAt, LocalTime.of(23, 59, 59)),
                type,
                context
            );
        }

        private LocalDateTime dateParse(String at, LocalTime time) {
            try {
                return LocalDateTime.of(
                    LocalDate.parse(at, DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    time
                );
            } catch (DateTimeParseException e) {
                throw new CoreException(ErrorType.BAD_REQUEST, "날짜 형식이 올바르지 않습니다.");
            }
        }
    }

    public record CreateResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        long rewardAmount
    ) {
        public static CreateResponse from(AdInfo.Create info) {
            return new CreateResponse(
                info.id(),
                info.name(),
                info.description(),
                info.imageUrl(),
                info.rewardAmount()
            );
        }
    }

    public record JoinResponse(
        Long adId,
        Long userId,
        LocalDateTime joinedAt
    ) {
        public static JoinResponse from(AdInfo.Join info) {
            return new JoinResponse(
                info.adId(),
                info.userId(),
                info.joinedAt()
            );
        }
    }

    public record GetJoinAbleResponse(
        List<GetJoinAbleItem> items
    ) {
        public static GetJoinAbleResponse from(AdInfo.GetJoinAble info) {
            return new GetJoinAbleResponse(
                info.items().stream()
                    .map(GetJoinAbleItem::from)
                    .toList()
            );
        }
    }

    private record GetJoinAbleItem(
        Long adId,
        String name,
        String description,
        String imageUrl,
        long rewardAmount
    ) {
        private static GetJoinAbleItem from(AdInfo.GetJoinAbleItem item) {
            return new GetJoinAbleItem(
                item.adId(),
                item.name(),
                item.description(),
                item.imageUrl(),
                item.rewardAmount()
            );
        }
    }

    public record GetJoinedHistoriesResponse(
        Long userId,
        int page,
        int size,
        List<GetJoinedHistoryItem> histories
    ) {
        public static GetJoinedHistoriesResponse from(AdInfo.GetJoinedHistories info) {
            return new GetJoinedHistoriesResponse(
                info.userId(),
                info.page(),
                info.size(),
                info.histories().stream()
                    .map(GetJoinedHistoryItem::from)
                    .toList()
            );
        }
    }

    private record GetJoinedHistoryItem(
        Long adId,
        Long userId,
        String name,
        LocalDateTime joinedAt,
        long rewardAmount
    ) {
        private static GetJoinedHistoryItem from(AdInfo.JoinedHistory joinedHistory) {
            return new GetJoinedHistoryItem(
                joinedHistory.adId(),
                joinedHistory.userId(),
                joinedHistory.name(),
                joinedHistory.joinedAt(),
                joinedHistory.rewardAmount()
            );
        }
    }
}
