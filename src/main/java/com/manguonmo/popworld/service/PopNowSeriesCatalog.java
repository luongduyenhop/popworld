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
                new StandardItemDef("Unknown Journey", RarityType.SECRET, "/images/popnow/hirono/unknown-journey.png", 10)
        )));

        // 2. Hirono The Other One (12 regular + 1 secret)
        catalog.put("hirono-the-other-one", new SeriesCatalogDef("hirono-the-other-one", 12, List.of(
                new StandardItemDef("Amnesia", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Amnesia.png", 100),
                new StandardItemDef("The Ghost", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/POP_MART_-_Hirono_The_Other_One_Series_Ghost_-_The_Ghost.png", 100),
                new StandardItemDef("Fox", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_The_Fox.png", 100),
                new StandardItemDef("Staring", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Staring.png", 100),
                new StandardItemDef("Marionette", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Marionette.png", 100),
                new StandardItemDef("The Monster", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_The_Monster.png", 100),
                new StandardItemDef("Being Alive", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Boxing_Alive.png", 100),
                new StandardItemDef("Nowhere Safe", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Nowhere_Safe.png", 100),
                new StandardItemDef("Vagrant", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Vagrancy.png", 100),
                new StandardItemDef("The Crow", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_The_Crow.png", 100),
                new StandardItemDef("Raving", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Raving.png", 100),
                new StandardItemDef("Cuckoo", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Cuckoo.png", 100),
                new StandardItemDef("Silent Scream", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_Dreaming_secret.png", 10)
        )));

        // 3. Labubu Fall in Wild (6 regular + 1 secret)
        catalog.put("fall-in-wild", new SeriesCatalogDef("fall-in-wild", 6, List.of(
                new StandardItemDef("Datura", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Datura_1800x1800.jpg", 100),
                new StandardItemDef("Bellflower", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Bellflower_1800x1800.jpg", 100),
                new StandardItemDef("Cactus", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Cactus_1800x1800.jpg", 100),
                new StandardItemDef("Monstera Deliciosa", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Monstera_Deliciosa_1800x1800.jpg", 100),
                new StandardItemDef("Platy Cerium", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Platy_Cerium_1800x1800.jpg", 100),
                new StandardItemDef("Crotalaria", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Crotalaria_1800x1800.jpg", 100),
                new StandardItemDef("Golden Forest King", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Gardener_Secret_1800x1800.jpg", 10)
        )));

        // 4. Labubu Have a Seat (6 regular + 1 secret)
        catalog.put("have-a-seat", new SeriesCatalogDef("have-a-seat", 6, List.of(
                new StandardItemDef("Dada", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Dada_1800x1800.png", 100),
                new StandardItemDef("Ququ", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Ququ_1800x1800.png", 100),
                new StandardItemDef("Sisi", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Sisi_1800x1800.png", 100),
                new StandardItemDef("Hehe", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Hehe_1800x1800.png", 100),
                new StandardItemDef("Zizi", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Zizi_1800x1800.png", 100),
                new StandardItemDef("Baba", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Baba_1800x1800.png", 100),
                new StandardItemDef("DuoDuo Chestnut", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Duoduo_secret_1800x1800.png", 10)
        )));

        // 5. Skullpanda City of Night (12 regular + 1 secret)
        catalog.put("city-of-night", new SeriesCatalogDef("city-of-night", 12, List.of(
                new StandardItemDef("Law Executor", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Law_Executor_1800x1800.jpg", 100),
                new StandardItemDef("Puppet Singer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Puppet_Singer_1800x1800.jpg", 100),
                new StandardItemDef("Ardent Youth", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Skullpanda_City_Of_Night_Ardent_Youth_1800x1800.jpg", 100),
                new StandardItemDef("Scroll Delivery", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Scroll_Delivery_1800x1800.jpg", 100),
                new StandardItemDef("Pet Cat", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Pet_Cat_1800x1800.jpg", 100),
                new StandardItemDef("Traveller", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Traveller_1800x1800.jpg", 100),
                new StandardItemDef("Heart Seeker", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Heart_Seeker_1800x1800.jpg", 100),
                new StandardItemDef("Naughty Bodyguard", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Naughty_Bodyguard_1800x1800.jpg", 100),
                new StandardItemDef("DJ Player", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_DJ_Player_1800x1800.jpg", 100),
                new StandardItemDef("The Princess", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_The_Princess_1800x1800.jpg", 100),
                new StandardItemDef("Meditator", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Meditator_1800x1800.jpg", 100),
                new StandardItemDef("Dancer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Skullpanda_City_Of_Night_Dancer_1800x1800.jpg", 100),
                new StandardItemDef("Guardian of Night", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/popmart_Skullpanda_City_Of_Night_Guardian_of_Night_secret_1800x1800.jpg", 10)
        )));

        // 6. Skullpanda The Warmth (12 regular + 1 secret)
        catalog.put("the-warmth", new SeriesCatalogDef("the-warmth", 12, List.of(
                new StandardItemDef("The Raining Day", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Raining_Day_1800x1800.jpg", 100),
                new StandardItemDef("The Encounter", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Encounter_1800x1800.jpg", 100),
                new StandardItemDef("The Day Off", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Day_Off_1800x1800.jpg", 100),
                new StandardItemDef("Enjoy Oneself", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Enjoy_Oneself_1800x1800.jpg", 100),
                new StandardItemDef("Mind With The Wind", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Mind_With_The_Wind_1800x1800.jpg", 100),
                new StandardItemDef("Doodling", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Doodling_1800x1800.jpg", 100),
                new StandardItemDef("Wandering", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Wandering_1800x1800.jpg", 100),
                new StandardItemDef("Recall The Past", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Recall_The_Past_1800x1800.jpg", 100),
                new StandardItemDef("Chirping", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Chirping_1800x1800.jpg", 100),
                new StandardItemDef("Loosening", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Loosening_1800x1800.jpg", 100),
                new StandardItemDef("Taste From The Memory", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Taste_From_The_Memory_1800x1800.jpg", 100),
                new StandardItemDef("The Scent", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Scent_1800x1800.jpg", 100),
                new StandardItemDef("The Warmth", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Warmth_secret_1800x1800.jpg", 10)
        )));

        // 7. Mega Space Molly 100% Series 2 (9 regular + 1 secret)
        catalog.put("space-molly", new SeriesCatalogDef("space-molly", 9, List.of(
                new StandardItemDef("Space Molly Bananaman", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/87221d51f26e77a08e388addbd6f0b51_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Melting", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/05f23838846685b9e6eac31ed835a68a_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Basquiat", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/926e3484a7587c8e39cafdb56a4f2196_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Keith Haring", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/3722d791b1b966bca0b8155f276c6779_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Heartfelt", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/eb114a19a00b495b7cb8073688755c9a_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Mint Chocolate", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/39ee4912734cf17a743fbbeec4ed4d71_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Toffee", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/ef7eb9f5b66d45a85b120836bfe2334a_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Cheerleader", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/107db3269a7c1bef66ae07d03e673a42_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Glacier", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/c726a28c0554c6126a1fcc5f957de382_1800x1800.jpg", 100),
                new StandardItemDef("Space Molly Galactic Star", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/be5597caf043c8bec9b8a74a06fd5778_1800x1800.jpg", 10)
        )));

        // 8. Dimoo Retro (12 regular + 1 secret)
        catalog.put("dimoo-retro", new SeriesCatalogDef("dimoo-retro", 12, List.of(
                new StandardItemDef("Angel", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Angel_1800x1800.jpg", 100),
                new StandardItemDef("Devil", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Devil_1800x1800.jpg", 100),
                new StandardItemDef("Flamingo", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Flamingo_1800x1800.jpg", 100),
                new StandardItemDef("Elk", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Elk_1800x1800.jpg", 100),
                new StandardItemDef("Little Green Dragon", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Little_Green_Dragon_1800x1800.jpg", 100),
                new StandardItemDef("Joker", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Joker_1800x1800.jpg", 100),
                new StandardItemDef("Magician", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Magician_1800x1800.jpg", 100),
                new StandardItemDef("Rocky Overlord", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Rocky_Overlord_1800x1800.jpg", 100),
                new StandardItemDef("Pajamas Rabbit", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Pajamas_Rabbit_1800x1800.jpg", 100),
                new StandardItemDef("Rocky King", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Rodky_King_1800x1800.jpg", 100),
                new StandardItemDef("Snowy Owl", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Snowy_Owl_1800x1800.jpg", 100),
                new StandardItemDef("Snowball", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Snowball_1800x1800.jpg", 100),
                new StandardItemDef("Golden Walkman", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Dark_Night_secret_1800x1800.jpg", 10)
        )));

        // 9. Crybaby Crying Parade (12 regular + 1 secret)
        catalog.put("crying-parade", new SeriesCatalogDef("crying-parade", 12, List.of(
                new StandardItemDef("The Letter", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blind_box_popmart_The_Letter_1800x1800.jpg", 100),
                new StandardItemDef("The Drummer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_The_Drummer_1800x1800.jpg", 100),
                new StandardItemDef("Peace Please", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Peace_Please_1800x1800.jpg", 100),
                new StandardItemDef("Monkey", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Monkey_1800x1800.jpg", 100),
                new StandardItemDef("Long Legged Clown", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Long_Legged_Clown_1800x1800.jpg", 100),
                new StandardItemDef("Keep Go Go", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Keep_Go_Go_1800x1800.jpg", 100),
                new StandardItemDef("Good Girl", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Good_Girl_1800x1800.jpg", 100),
                new StandardItemDef("Free Lion", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_Free_Lion_1800x1800.jpg", 100),
                new StandardItemDef("Fall", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_fall_1800x1800.jpg", 100),
                new StandardItemDef("Yes Can Can", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_yes_can_can_1800x1800.jpg", 100),
                new StandardItemDef("Ugly Duckling", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_blindbox_ugly_duckling_1800x1800.jpg", 100),
                new StandardItemDef("The Trumpet", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_blindbox_The_Trumpet_1800x1800.jpg", 100),
                new StandardItemDef("The Saddest King", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_The_Saddest_King_secret_1800x1800.jpg", 10)
        )));

        // 10. Crybaby Sad Club (6 regular + 1 secret)
        catalog.put("sad-club", new SeriesCatalogDef("sad-club", 6, List.of(
                new StandardItemDef("Big Cleaning Day", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Big_Cleaning_Day_1800x1800.jpg", 100),
                new StandardItemDef("Teardrops On The Pillow", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Teardrops_On_The_Pillow_1800x1800.jpg", 100),
                new StandardItemDef("The Hottest Day Of Summer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_The_Hottest_Day_Of_Summer_1800x1800.jpg", 100),
                new StandardItemDef("Withering Flower", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Withering_Flower_1800x1800.jpg", 100),
                new StandardItemDef("Teardrop Bowl", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Teardrop_Bowl_1800x1800.jpg", 100),
                new StandardItemDef("Devastated", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Devastated_1800x1800.jpg", 100),
                new StandardItemDef("A Sad Show", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_A_Sad_Show_Secret_1800x1800.jpg", 10)
        )));

        // 11. Dimoo Dating (6 regular + 1 secret)
        catalog.put("dimoo-dating", new SeriesCatalogDef("dimoo-dating", 6, List.of(
                new StandardItemDef("Ice Cream", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Ice_Cream_1800x1800.jpg", 100),
                new StandardItemDef("Marshmallow", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Marshmallow_1800x1800.jpg", 100),
                new StandardItemDef("Love Theatre", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Love_Theatre_1800x1800.jpg", 100),
                new StandardItemDef("Love Fountain", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Love_Fountain_1800x1800.jpg", 100),
                new StandardItemDef("Joyriding Bumper Car", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Joyriding_Bumper_Car_1800x1800.jpg", 100),
                new StandardItemDef("Record Anniversary", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Record_Anniversary_1800x1800.jpg", 100),
                new StandardItemDef("Photo Prop Wall", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Photo_Prop_Wall_secret_1800x1800.jpg", 10)
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
