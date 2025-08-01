package com.discphy.ad.interfaces.api.ad;

import com.discphy.ad.domain.ad.Ad;
import com.discphy.ad.domain.ad.AdCommand;
import com.discphy.ad.domain.ad.AdJoinConditionType;
import com.discphy.ad.domain.ad.AdJoinedHistory;
import com.discphy.ad.domain.user.User;
import com.discphy.ad.infrastructure.ad.jpa.AdJoinedUserJpaRepository;
import com.discphy.ad.infrastructure.ad.jpa.AdJpaRepository;
import com.discphy.ad.infrastructure.user.jpa.UserJpaRepository;
import com.discphy.ad.interfaces.api.ApiResponse;
import com.discphy.ad.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdV1ControllerE2ETest {

    @Autowired
    private AdJpaRepository adJpaRepository;

    @Autowired
    private AdJoinedUserJpaRepository adJoinedUserJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    public static final String BASE_URL = "/api/v1/ads";

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create("테스트 사용자");
        userJpaRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/ads")
    @Nested
    class Create {

        @DisplayName("광고명이 중복 될 경우, 광고 생성에 실패한다.")
        @Test
        void createWithDuplicateName() {
            // given
            String name = "중복 광고명";

            AdCommand.Create command = new AdCommand.Create(
                name,
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            adJpaRepository.save(Ad.create(command));
            assertThat(adJpaRepository.findByName(name)).isPresent();

            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                name,
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                "2023-01-01",
                "2023-12-31",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.CreateResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<AdV1Dto.CreateResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is4xxClientError()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getMessage()).isEqualTo("이미 존재하는 광고명입니다.")
            );
        }

        @DisplayName("광고 조건이 유효하지 않으면, 광고 생성에 실패한다.")
        @Test
        void createWithInvalidCondition() {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "유효하지 않은 광고",
                1000L,
                1,
                "광고 설명",
                "https://example.com/image.jpg",
                "2023-01-01",
                "2023-12-31",
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 0
                }
                """
            );

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.CreateResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<AdV1Dto.CreateResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is4xxClientError()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getMessage()).isEqualTo("광고 참여 조건이 유효하지 않습니다.")
            );
        }

        @DisplayName("광고를 생성한다.")
        @Test
        void create() {
            // given
            AdV1Dto.CreateRequest request = new AdV1Dto.CreateRequest(
                "새로운 광고",
                2000L,
                5,
                "새로운 광고 설명",
                "https://example.com/new-image.jpg",
                "2023-01-01",
                "2023-12-31",
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.CreateResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<AdV1Dto.CreateResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().id()).isNotNull()
            );
        }
    }

    @DisplayName("POST /api/v1/ads/{adId}/join")
    @Nested
    class Join {

        public static final Function<Long, String> getUrl = (id) -> "/api/v1/ads/" + id + "/join";

        @DisplayName("광고가 존재하지 않으면 실패한다.")
        @Test
        void notExistsAd() {
            // given
            Long adId = 999L; // 존재하지 않는 광고 ID
            Long userId = user.getId();

            assertThat(adJpaRepository.findById(adId)).isNotPresent();

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.JoinResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.JoinResponse>> response =
                restTemplate.exchange(getUrl.apply(adId), HttpMethod.POST, new HttpEntity<>(null, createHeaders(userId)), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is4xxClientError()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getMessage()).isEqualTo("존재하지 않는 광고입니다.")
            );
        }

        @DisplayName("광고 참여 횟수가 0인 경우, 참여에 실패한다.")
        @Test
        void joinWithZeroJoinCount() {
            // given
            Ad ad = Ad.create(new AdCommand.Create(
                "테스트 광고",
                1000L,
                1,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            ad.join();
            adJpaRepository.save(ad);

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.JoinResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.JoinResponse>> response =
                restTemplate.exchange(getUrl.apply(ad.getId()), HttpMethod.POST, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is4xxClientError()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getMessage()).isEqualTo("참여가 불가능합니다.")
            );
        }

        @DisplayName("광고 참요 조건에 맞지 않는 경우, 참여에 실패한다.")
        @Test
        void joinWithInvalidCondition() {
            // given
            Ad ad = Ad.create(new AdCommand.Create(
                "테스트 광고",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 2
                }
                """
            ));
            adJpaRepository.save(ad);

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.JoinResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.JoinResponse>> response =
                restTemplate.exchange(getUrl.apply(ad.getId()), HttpMethod.POST, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is4xxClientError()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getMessage()).isEqualTo("광고 참여 조건을 만족하지 않습니다.")
            );
        }

        @DisplayName("광고 참여에 성공한다. - 참여 조건이 최초 참여")
        @Test
        void joinSuccessWithFirstJoinCondition() {
            // given
            Ad ad = Ad.create(new AdCommand.Create(
                "테스트 광고",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.save(ad);

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.JoinResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.JoinResponse>> response =
                restTemplate.exchange(getUrl.apply(ad.getId()), HttpMethod.POST, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().adId()).isEqualTo(ad.getId()),
                () -> assertThat(response.getBody().getData().userId()).isEqualTo(user.getId()),
                () -> assertThat(response.getBody().getData().joinedAt()).isNotNull()
            );
        }

        @DisplayName("광고 참여에 성공한다. - 참여 조건이 N번이상")
        @Test
        void joinSuccessWithCountOverCondition() {
            // given
            Ad ad = Ad.create(new AdCommand.Create(
                "테스트 광고",
                1000L,
                10,
                "광고 설명",
                "https://example.com/image.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 2
                }
                """
            ));
            adJpaRepository.save(ad);

            AdJoinedHistory history1 = AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(1));
            AdJoinedHistory history2 = AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(2));
            adJoinedUserJpaRepository.saveAll(List.of(history1, history2));

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.JoinResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.JoinResponse>> response =
                restTemplate.exchange(getUrl.apply(ad.getId()), HttpMethod.POST, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().adId()).isEqualTo(ad.getId()),
                () -> assertThat(response.getBody().getData().userId()).isEqualTo(user.getId()),
                () -> assertThat(response.getBody().getData().joinedAt()).isNotNull()
            );
        }

        @DisplayName("광고 참여에 성공한다. - 특정 광고 ID 참가 이력")
        @Test
        void joinSuccessWithSpecificAdId() {
            // given
            Ad specificAd = Ad.create(new AdCommand.Create(
                "특정 광고",
                1500L,
                5,
                "특정 광고 설명",
                "https://example.com/specific-ad.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.save(specificAd);
            adJoinedUserJpaRepository.save(AdJoinedHistory.create(specificAd, user, LocalDateTime.now().minusDays(1)));

            Ad ad = Ad.create(new AdCommand.Create(
                "광고",
                1500L,
                5,
                "특정 광고 설명",
                "https://example.com/specific-ad.jpg",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                AdJoinConditionType.SPECIFIC_AD_ID,
                String.format("""
                {
                    "adId": %d
                }
                """, specificAd.getId())
            ));
            adJpaRepository.save(ad);

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.JoinResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.JoinResponse>> response =
                restTemplate.exchange(getUrl.apply(ad.getId()), HttpMethod.POST, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().adId()).isEqualTo(ad.getId()),
                () -> assertThat(response.getBody().getData().userId()).isEqualTo(user.getId()),
                () -> assertThat(response.getBody().getData().joinedAt()).isNotNull()
            );
        }
    }

    @DisplayName("GET /api/v1/ads")
    @Nested
    class GetJoinable {

        @DisplayName("최대 10개의 광고를 조회한다.")
        @Test
        void withMaxLimit() {
            // given
            List<Ad> list = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                Ad ad = Ad.create(new AdCommand.Create(
                    "광고 " + i,
                    1_000L + i,
                    10 + i,
                    "광고 설명 " + i,
                    "https://example.com/image" + i + ".jpg",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    AdJoinConditionType.FIRST_JOIN,
                    null
                ));
                list.add(ad);
            }
            adJpaRepository.saveAll(list);

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.GetJoinAbleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.GetJoinAbleResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.GET, new HttpEntity<>(createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().items()).hasSize(10),
                () -> assertThat(response.getBody().getData().items())
                    .extracting("name")
                    .containsExactly("광고 10", "광고 9", "광고 8", "광고 7", "광고 6", "광고 5", "광고 4", "광고 3", "광고 2", "광고 1")
            );
        }

        @DisplayName("참여 가능한 광고만 조회한다.")
        @Test
        void onlyJoinable() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad joinableAd = Ad.create(new AdCommand.Create(
                "참여 가능한 광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            Ad countOverConditionAd = Ad.create(new AdCommand.Create(
                "참여 횟수 조건 광고",
                1_500L,
                50,
                "참여 횟수 조건 광고 설명",
                "https://example.com/count_over.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.COUNT_OVER,
                """
                {
                    "joinCount": 2
                }
                """
            ));
            Ad specificAdIdConditionAd = Ad.create(new AdCommand.Create(
                "특정 광고 이력 조건 광고",
                2_500L,
                30,
                "특정 광고 이력 조건 광고 설명",
                "https://example.com/specific_ad.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.SPECIFIC_AD_ID,
                """
                {
                    "adId": 12345
                }
                """
            ));
            Ad notDisplayedAd = Ad.create(new AdCommand.Create(
                "노출되지 않는 광고",
                1_000L,
                50,
                "노출되지 않는 광고 설명",
                "https://example.com/not_displayed.jpg",
                now.minusDays(2),
                now.minusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            Ad notExistJoinCountAd = Ad.create(new AdCommand.Create(
                "참여 불가능한 광고",
                3_000L,
                1,
                "참여 불가능한 광고 설명",
                "https://example.com/not_joinable.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            notExistJoinCountAd.join(); // 참여 카운트가 0이 되도록 설정

            adJpaRepository.saveAll(List.of(
                joinableAd, countOverConditionAd, specificAdIdConditionAd, notDisplayedAd, notExistJoinCountAd
            ));

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.GetJoinAbleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.GetJoinAbleResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.GET, new HttpEntity<>(createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().items()).hasSize(1)
                    .extracting("name")
                    .containsExactly(joinableAd.getName())
            );
        }

        @DisplayName("적립 액수가 높은 순서로 정렬되어야 한다.")
        @Test
        void orderByRewardAmountDesc() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Ad ad1 = Ad.create(new AdCommand.Create(
                "광고1",
                3_000L,
                100,
                "광고1 설명",
                "https://example.com/ad1.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            Ad ad2 = Ad.create(new AdCommand.Create(
                "광고2",
                5_000L,
                100,
                "광고2 설명",
                "https://example.com/ad2.jpg",
                now.minusDays(1),
                now.plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            ));
            adJpaRepository.saveAll(List.of(ad1, ad2));

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.GetJoinAbleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.GetJoinAbleResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.GET, new HttpEntity<>(createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().items()).hasSize(2)
                    .extracting("rewardAmount")
                    .containsExactly(
                        ad2.getRewardAmount(),
                        ad1.getRewardAmount()
                    )
            );
        }
    }

    @DisplayName("GET /api/v1/ads/histories")
    @Nested
    class GetHistories {

        public static final String BASE_URL = "/api/v1/ads/histories";

        @DisplayName("광고 참여 시각이 오래된 순으로 정렬되어야 한다.")
        @Test
        void orderByJoinedAtAsc() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            adJpaRepository.save(ad);

            AdJoinedHistory joinedUser1 = AdJoinedHistory.create(ad, user, LocalDateTime.of(2025, 7, 26, 13, 0, 1));
            AdJoinedHistory joinedUser2 = AdJoinedHistory.create(ad, user, LocalDateTime.of(2025, 7, 26, 13, 0, 0));
            adJoinedUserJpaRepository.saveAll(List.of(joinedUser1, joinedUser2));

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.GetJoinedHistoriesResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.GetJoinedHistoriesResponse>> response =
                restTemplate.exchange(BASE_URL, HttpMethod.GET, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            System.out.println("response = " + response);
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().histories()).hasSize(2)
                    .extracting("joinedAt")
                    .containsExactly(
                        LocalDateTime.of(2025, 7, 26, 13, 0, 0),
                        LocalDateTime.of(2025, 7, 26, 13, 0, 1)
                    )
            );
        }

        @DisplayName("페이지네이션이 적용 되어야 한다.")
        @Test
        void pagination() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            adJpaRepository.save(ad);

            for (int i = 0; i < 10; i++) {
                AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(i));
                adJoinedUserJpaRepository.save(joinedUser);
            }

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.GetJoinedHistoriesResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.GetJoinedHistoriesResponse>> response =
                restTemplate.exchange(BASE_URL + "?page=1&size=3", HttpMethod.GET, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().histories()).hasSize(3),
                () -> assertThat(response.getBody().getData().page()).isEqualTo(1),
                () -> assertThat(response.getBody().getData().size()).isEqualTo(3)
            );
        }

        @DisplayName("최대 50개까지만 이력을 조회할 수 있다.")
        @Test
        void maxLimit() {
            // given
            AdCommand.Create command = new AdCommand.Create(
                "광고명",
                2_000L,
                100,
                "새로운 광고 설명",
                "https://example.com/image2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AdJoinConditionType.FIRST_JOIN,
                null
            );

            Ad ad = Ad.create(command);
            adJpaRepository.save(ad);

            for (int i = 0; i < 50; i++) {
                AdJoinedHistory joinedUser = AdJoinedHistory.create(ad, user, LocalDateTime.now().minusDays(i));
                adJoinedUserJpaRepository.save(joinedUser);
            }

            // when
            ParameterizedTypeReference<ApiResponse<AdV1Dto.GetJoinedHistoriesResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AdV1Dto.GetJoinedHistoriesResponse>> response =
                restTemplate.exchange(BASE_URL + "?page=1&size=30", HttpMethod.GET, new HttpEntity<>(null, createHeaders(user.getId())), responseType);

            // then
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().getData()).isNotNull(),
                () -> assertThat(response.getBody().getData().histories()).hasSize(30),
                () -> assertThat(response.getBody().getData().page()).isEqualTo(1),
                () -> assertThat(response.getBody().getData().size()).isEqualTo(30)
            );
        }
    }

    private MultiValueMap<String, String> createHeaders(Long userId) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("X-USER-ID", String.valueOf(userId));

        return headers;
    }
}