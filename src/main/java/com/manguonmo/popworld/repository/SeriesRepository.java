package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findByName(String name);
    List<Series> findByCharacterIpId(Long characterIpId);
    List<Series> findAllByOrderByReleaseDateDesc();
}