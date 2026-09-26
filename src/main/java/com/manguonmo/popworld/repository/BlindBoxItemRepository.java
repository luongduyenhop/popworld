package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.BlindBoxItem;
import com.manguonmo.popworld.entity.RarityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlindBoxItemRepository extends JpaRepository<BlindBoxItem, Long> {
    List<BlindBoxItem> findByProductIdAndActiveTrue(Long productId);
    List<BlindBoxItem> findByProductId(Long productId);
    List<BlindBoxItem> findByProductIdAndRarity(Long productId, RarityType rarity);
    long countByProductIdAndActiveTrue(Long productId);
}
