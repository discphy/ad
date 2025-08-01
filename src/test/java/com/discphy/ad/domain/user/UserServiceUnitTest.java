package com.discphy.ad.domain.user;

import com.discphy.ad.exception.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @DisplayName("사용자 조회 시, ")
    @Nested
    class Get {

        @DisplayName("존재하지 않는 사용자 ID를 주면, 예외가 발생한다.")
        @Test
        void notExistUser() {
            // given
            Long userId = 999L;

             when(userRepository.findById(userId))
                 .thenReturn(Optional.empty());

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
            when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

            // when
            User foundUser = userService.get(user.getId());

            // then
            assertThat(foundUser).isNotNull();
        }
    }

}