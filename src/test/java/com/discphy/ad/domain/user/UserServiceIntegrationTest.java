package com.discphy.ad.domain.user;

import com.discphy.ad.exception.CoreException;
import com.discphy.ad.infrastructure.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @AfterEach
    void tearDown() {
        userJpaRepository.deleteAllInBatch();
    }

    @DisplayName("사용자 조회 시, ")
    @Nested
    class Get {

        @DisplayName("존재하지 않는 사용자 ID를 주면, 예외가 발생한다.")
        @Test
        void notExistUser() {
            // given
            Long userId = 999L;
            assertThat(userJpaRepository.findById(userId)).isNotPresent();

            // when & then
            assertThatThrownBy(() -> userService.get(userId))
                .isInstanceOf(CoreException.class)
                .hasMessage("사용자가 존재하지 않습니다.");
        }

        @DisplayName("사용자를 조회한다.")
        @Test
        void get() {
            // given
            User user = User.create("카카오페이");
            userJpaRepository.save(user);

            // when
            User foundUser = userService.get(user.getId());

            // then
            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getId()).isEqualTo(user.getId());
        }
    }
}