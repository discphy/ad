package com.discphy.ad.interfaces.api.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.discphy.ad.application.ad.AdFacade;
import com.discphy.ad.domain.ad.AdCommand;
import com.discphy.ad.domain.ad.AdInfo;
import com.discphy.ad.domain.ad.AdJoinConditionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.function.Function;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
    AdV1Controller.class
})
class AdV1ControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdFacade adFacade;

    public static final String BASE_URL = "/api/v1/ads";

    @DisplayName("POST /api/v1/ads")
    @Nested
    class Create {

        @DisplayName("광고명이 비어있으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notBlankName() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                "2025-07-28",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("광고명은 비어있을 수 없습니다."));
        }

        @DisplayName("광고 참여 적립 액수가 존재하지 않으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullRewardAmount() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                null,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                "2025-07-28",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("광고 참여 적립 액수는 필수입니다."));
        }

        @DisplayName("광고 참여 가능 횟수가 존재하지 않으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullJoinCount() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                null,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                "2025-07-28",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("광고 참여 가능 횟수는 필수입니다."));
        }

        @DisplayName("광고 노출 시작 시간이 존재하지 않으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullStartedAt() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                null,
                "2025-07-28",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("광고 노출 시작 시간은 필수입니다."));
        }

        @DisplayName("광고 노출 종료 시간이 존재하지 않으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullEndedAt() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                null,
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("광고 노출 종료 시간은 필수입니다."));
        }

        @DisplayName("광고 노출 시작 시간이 yyyy-MM-dd 형식이 아니면 400 Bad Request 에러가 발생한다.")
        @Test
        void invalidStartedAtFormat() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025/07/27", // 잘못된 형식
                "2025-07-28",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("날짜 형식이 올바르지 않습니다."));
        }

        @DisplayName("광고 노출 종료 시간이 yyyy-MM-dd 형식이 아니면 400 Bad Request 에러가 발생한다.")
        @Test
        void invalidEndedAtFormat() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                "2025/07/28", // 잘못된 형식
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("날짜 형식이 올바르지 않습니다."));
        }

        @DisplayName("광고 참여 조건이 비어있으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullAdJoinConditionType() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                "2025-07-28",
                null, // 비어있는 참여 조건
                null
            );

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("광고 참여 조건은 필수입니다."));
        }

        @DisplayName("광고 생성 시, 광고 정보가 정상적으로 반환된다.")
        @Test
        void create() throws Exception {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "광고명",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2025-07-27",
                "2025-07-28",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            AdInfo.Create adInfo = new AdInfo.Create(
                1L,
                "광고명",
                "광고 설명",
                "https://example.com/image.jpg",
                1000L
            );

            when(adFacade.create(request.toCommand()))
                .thenReturn(adInfo);

            // when & then
            mockMvc.perform(
                    post(BASE_URL)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("광고명"))
                .andExpect(jsonPath("$.data.description").value("광고 설명"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.data.rewardAmount").value(1000));
        }

    }

    @DisplayName("POST /api/v1/ads/{adId}/join")
    @Nested
    class Join {

        public static final Function<Long, String> getUrl = (id) -> "/api/v1/ads/" + id + "/join";

        @DisplayName("userId 헤더가 비어있으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullUserId() throws Exception {
            // given
            Long adId = 1L;

            // when & then
            mockMvc.perform(
                    post(getUrl.apply(adId))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("X-USER-ID 헤더가 누락되었습니다."));
        }

        @DisplayName("광고 참여 시, 광고 참여 정보가 정상적으로 반환된다.")
        @Test
        void join() throws Exception {
            // given
            Long adId = 1L;
            Long userId = 10L;

            AdInfo.Join adInfo = new AdInfo.Join(
                adId,
                userId,
                LocalDateTime.now()
            );

            when(adFacade.join(AdCommand.Join.of(adId, userId)))
                .thenReturn(adInfo);

            // when & then
            mockMvc.perform(
                    post(getUrl.apply(adId))
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.joinedAt").isNotEmpty());
        }
    }


    @DisplayName("GET /api/v1/ads")
    @Nested
    class GetJoinable {

        @DisplayName("userId 헤더가 비어있으면 400 Bad Request 에러가 발생한다.")
        @Test
        void notNullUserId() throws Exception {
            // when & then
            mockMvc.perform(
                    get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("X-USER-ID 헤더가 누락되었습니다."));
        }
    }
}