package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.PointSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointSettingRepository extends JpaRepository<PointSetting, Long> {
    Optional<PointSetting> findFirstByOrderByIdAsc();
}
