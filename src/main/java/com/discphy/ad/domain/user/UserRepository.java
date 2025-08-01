package com.discphy.ad.domain.user;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long userId);
}
