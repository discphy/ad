package com.discphy.ad.infrastructure.user;

import com.discphy.ad.domain.user.User;
import com.discphy.ad.domain.user.UserRepository;
import com.discphy.ad.infrastructure.user.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId);
    }
}
