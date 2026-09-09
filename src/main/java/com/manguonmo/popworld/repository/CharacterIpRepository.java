package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.CharacterIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterIpRepository extends JpaRepository<CharacterIp, Long> {
    List<CharacterIp> findByArtistId(Long artistId);
}