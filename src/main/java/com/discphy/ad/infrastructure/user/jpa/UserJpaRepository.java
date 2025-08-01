package com.discphy.ad.infrastructure.user.jpa;

import com.discphy.ad.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
}
