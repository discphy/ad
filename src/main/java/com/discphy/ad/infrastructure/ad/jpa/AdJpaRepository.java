package com.discphy.ad.infrastructure.ad.jpa;

import com.discphy.ad.domain.ad.Ad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdJpaRepository extends JpaRepository<Ad, Long> {
    Optional<Ad> findByName(String name);

    @Query("""
        SELECT a FROM Ad a
        WHERE a.joinCount > 0 AND a.startedAt <= :dateTime AND a.endedAt >= :dateTime
        ORDER BY a.rewardAmount DESC
    """)
    List<Ad> findJoinableAds(LocalDateTime dateTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Ad a WHERE a.id = :id")
    Optional<Ad> findByIdWithLock(Long id);

    boolean existsByName(String name);
}
