package com.discphy.ad.infrastructure.ad.jpa;

import com.discphy.ad.domain.ad.AdJoinedHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdJoinedUserJpaRepository extends JpaRepository<AdJoinedHistory, Long> {

    List<AdJoinedHistory> findByUserIdOrderByJoinedAtAsc(Long userId, Pageable pageable);

    List<AdJoinedHistory> findByUserId(Long userId);
}
