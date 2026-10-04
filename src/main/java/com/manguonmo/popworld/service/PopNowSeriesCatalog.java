package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.RarityType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PopNowSeriesCatalog {

    public record StandardItemDef(String name, RarityType rarity, String imageUrl, int weight) {}

    public static class SeriesCatalogDef {
        private final String seriesKey;
        private final int boxesPerSet;
        private final List<StandardItemDef> items;

        public SeriesCatalogDef(String seriesKey, int boxesPerSet, List<StandardItemDef> items) {
            this.seriesKey = seriesKey;
            this.boxesPerSet = boxesPerSet;
            this.items = items;
        }

        public String getSeriesKey() {
            return seriesKey;
        }

        public int getBoxesPerSet() {
            return boxesPerSet;
        }

        public List<StandardItemDef> getItems() {
            return items;
        }
    }

    private final Map<String, SeriesCatalogDef> catalog = new LinkedHashMap<>();

    public PopNowSeriesCatalog() {
        initCatalog();
    }

    private void initCatalog() {
        // 1. Hirono Little Mischief (12 regular + 1 secret)
        catalog.put("hirono-little-mischief", new SeriesCatalogDef("hirono-little-mischief", 12, List.of(
                new StandardItemDef("Ragpicker", RarityType.REGULAR, "/images/popnow/hirono/ragpicker.png", 100),
                new StandardItemDef("Loose Fish", RarityType.REGULAR, "/images/popnow/hirono/loose-fish.png", 100),
                new StandardItemDef("Manacle", RarityType.REGULAR, "/images/popnow/hirono/manacle.png", 100),
                new StandardItemDef("The Aviator", RarityType.REGULAR, "/images/popnow/hirono/the-aviator.png", 100),
                new StandardItemDef("Protector", RarityType.REGULAR, "/images/popnow/hirono/protector.png", 100),
                new StandardItemDef("Persona", RarityType.REGULAR, "/images/popnow/hirono/persona.png", 100),
                new StandardItemDef("Robot", RarityType.REGULAR, "/images/popnow/hirono/robot.png", 100),
                new StandardItemDef("Birdman", RarityType.REGULAR, "/images/popnow/hirono/birdman.png", 100),
                new StandardItemDef("Destroyer", RarityType.REGULAR, "/images/popnow/hirono/destroyer.png", 100),
                new StandardItemDef("Pretender", RarityType.REGULAR, "/images/popnow/hirono/pretender.png", 100),
                new StandardItemDef("Boiling Frog", RarityType.REGULAR, "/images/popnow/hirono/boiling-frog.png", 100),
                new StandardItemDef("Float", RarityType.REGULAR, "/images/popnow/hirono/float.png", 100),
                new StandardItemDef("Dreaming", RarityType.SECRET, "/images/popnow/hirono/dreaming.png", 10)
        )));

        // 2. Hirono The Other One (12 regular + 1 secret)
        catalog.put("hirono-the-other-one", new SeriesCatalogDef("hirono-the-other-one", 12, List.of(
                new StandardItemDef("Amnesia", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("The Ghost", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Staring", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Marionette", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("The Monster", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Fox", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Vagrant", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("The Crow", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Puppet", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Being Alive", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Nowhere", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Silence", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 100),
                new StandardItemDef("Dreamer", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2", 10)
        )));

        // 3. Labubu Fall in Wild (6 regular + 1 secret)
        catalog.put("fall-in-wild", new SeriesCatalogDef("fall-in-wild", 6, List.of(
                new StandardItemDef("Camp Fire Labubu", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 100),
                new StandardItemDef("Fisherman Labubu", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 100),
                new StandardItemDef("Hiker Labubu", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 100),
                new StandardItemDef("Gardener Labubu", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 100),
                new StandardItemDef("Picnic Labubu", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 100),
                new StandardItemDef("Explorer Labubu", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 100),
                new StandardItemDef("Golden Forest King", RarityType.SECRET, "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg", 10)
        )));

        // 4. Labubu Have a Seat (6 regular + 1 secret)
        catalog.put("have-a-seat", new SeriesCatalogDef("have-a-seat", 6, List.of(
                new StandardItemDef("Dada", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 100),
                new StandardItemDef("Ququ", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 100),
                new StandardItemDef("Sisi", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 100),
                new StandardItemDef("Hehe", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 100),
                new StandardItemDef("Zizi", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 100),
                new StandardItemDef("Baba", RarityType.REGULAR, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 100),
                new StandardItemDef("DuoDuo Chestnut", RarityType.SECRET, "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg", 10)
        )));

        // 5. Skullpanda City of Night (12 regular + 1 secret)
        catalog.put("city-of-night", new SeriesCatalogDef("city-of-night", 12, List.of(
                new StandardItemDef("The Neon", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Mist", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Patrol", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Dancer", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Singer", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The DJ", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The City Police", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Navigator", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Driver", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Waiter", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Heartseeker", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("The Barber", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 100),
                new StandardItemDef("Night Walker", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2", 10)
        )));

        // 6. Skullpanda The Warmth (12 regular + 1 secret)
        catalog.put("the-warmth", new SeriesCatalogDef("the-warmth", 12, List.of(
                new StandardItemDef("The Scent", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Day Off", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Drowsiness", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Warmth", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Encounter", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Cozy", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Taste", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Chirping", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Loosening", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Mind", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Care", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Rebirth", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 100),
                new StandardItemDef("The Sun", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2", 10)
        )));

        // 7. Mega Space Molly 100% Series 2 (9 regular + 1 secret)
        catalog.put("space-molly", new SeriesCatalogDef("space-molly", 9, List.of(
                new StandardItemDef("Space Molly Bananaman", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Melting", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Basquiat", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Heart", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Galaxy", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Mint", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Cheerleader", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Ice Cream", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Rainbow", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 100),
                new StandardItemDef("Space Molly Christmas", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2", 10)
        )));

        // 8. Dimoo Retro (12 regular + 1 secret)
        catalog.put("dimoo-retro", new SeriesCatalogDef("dimoo-retro", 12, List.of(
                new StandardItemDef("Dimoo Bấm Nút", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Băng Cassette", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Máy Ảnh Cơ", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Đĩa Than", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Tivi Cổ", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Điện Thoại Bàn", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Radio Cũ", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Đồng Hồ Báo Thức", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Đèn Dầu", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Quạt Con Cóc", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Máy Đánh Chữ", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Hộp Nhạc", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dimoo Du Hành Vũ Trụ", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 10)
        )));

        // 9. Crybaby Crying Parade (12 regular + 1 secret)
        catalog.put("crying-parade", new SeriesCatalogDef("crying-parade", 12, List.of(
                new StandardItemDef("Crying Clown", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Cheerleader", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Magician", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Ballerina", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Drummer", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Acrobat", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Ringmaster", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Juggler", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Mime", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Fire-eater", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Strongman", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Crying Stilt-walker", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Vịt Vàng Khóc Nhè", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 10)
        )));

        // 10. Crybaby Sad Club (6 regular + 1 secret)
        catalog.put("sad-club", new SeriesCatalogDef("sad-club", 6, List.of(
                new StandardItemDef("Rainy Day Crybaby", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Broken Heart Crybaby", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Blue Monday Crybaby", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Lonely Star Crybaby", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Teardrop Crybaby", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Hug Me Crybaby", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 100),
                new StandardItemDef("Cầu Vồng Nước Mắt", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2", 10)
        )));

        // 11. Dimoo Dating (6 regular + 1 secret)
        catalog.put("dimoo-dating", new SeriesCatalogDef("dimoo-dating", 6, List.of(
                new StandardItemDef("Xem Phim Rạp", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Hẹn Hò Cà Phê", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Dạo Công Viên", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Đạp Xe Đôi", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Bữa Tiệc Picnic", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Ngắm Hoàng Hôn", RarityType.REGULAR, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 100),
                new StandardItemDef("Tỏ Tình Dưới Mưa", RarityType.SECRET, "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2", 10)
        )));
    }

    public Optional<SeriesCatalogDef> findCatalog(String slug, String seriesName, String productName) {
        String combined = ((slug != null ? slug : "") + " " +
                (seriesName != null ? seriesName : "") + " " +
                (productName != null ? productName : "")).toLowerCase();

        for (Map.Entry<String, SeriesCatalogDef> entry : catalog.entrySet()) {
            String key = entry.getKey();
            if (combined.contains(key) ||
                    (key.contains("hirono-little-mischief") && combined.contains("mischief")) ||
                    (key.contains("hirono-the-other-one") && (combined.contains("other one") || combined.contains("the-other-one"))) ||
                    (key.contains("fall-in-wild") && combined.contains("wild")) ||
                    (key.contains("have-a-seat") && combined.contains("seat")) ||
                    (key.contains("city-of-night") && combined.contains("night")) ||
                    (key.contains("the-warmth") && combined.contains("warmth")) ||
                    (key.contains("space-molly") && (combined.contains("molly") || combined.contains("space"))) ||
                    (key.contains("dimoo-retro") && combined.contains("retro")) ||
                    (key.contains("crying-parade") && combined.contains("parade")) ||
                    (key.contains("sad-club") && combined.contains("sad")) ||
                    (key.contains("dimoo-dating") && combined.contains("dating"))) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public boolean hasCatalog(String slug, String seriesName, String productName) {
        return findCatalog(slug, seriesName, productName).isPresent();
    }
}
