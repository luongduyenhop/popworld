package com.manguonmo.popworld.service;

public interface DatabaseSeedService {
    /**
     * Reseeds and synchronizes high-fidelity official POP MART catalog data:
     * - Fixes dynamic packaging (boxesPerSet: 6, 8, 9, 12, 16)
     * - Synchronizes complete characters for all series (blind_box_items)
     * - Initializes accurate slot inventory (blind_box_slots)
     * - Strips blind box slots and items from non-blind-box items (Mega & Accessories)
     * - Seeds authentic customer reviews and ratings
     */
    void reseedOfficialPopMartData();
}
