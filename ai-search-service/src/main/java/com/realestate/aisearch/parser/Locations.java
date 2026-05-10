package com.realestate.aisearch.parser;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Curated city/district list and theme keyword sets.
 * Same lists are referenced by the LLM system prompt so both parsers agree
 * on the set of valid locations.
 */
final class Locations {

    private Locations() {}

    /** Canonical Vietnamese city names. */
    static final List<String> CITIES = List.of(
            "Hà Nội",
            "TP. Hồ Chí Minh",
            "Đà Nẵng",
            "Hải Phòng",
            "Cần Thơ",
            "Nha Trang",
            "Vũng Tàu",
            "Huế",
            "Biên Hòa",
            "Thủ Đức",
            "Phú Quốc",
            "Quy Nhơn",
            "Hạ Long",
            "Đà Lạt",
            "Buôn Ma Thuột",
            "Vinh"
    );

    /** Aliases (folded form -> canonical city). */
    static final Map<String, String> CITY_ALIASES = Map.ofEntries(
            Map.entry("ha noi", "Hà Nội"),
            Map.entry("hanoi", "Hà Nội"),
            Map.entry("hn", "Hà Nội"),
            Map.entry("ho chi minh", "TP. Hồ Chí Minh"),
            Map.entry("hcm", "TP. Hồ Chí Minh"),
            Map.entry("hcmc", "TP. Hồ Chí Minh"),
            Map.entry("saigon", "TP. Hồ Chí Minh"),
            Map.entry("sai gon", "TP. Hồ Chí Minh"),
            Map.entry("tp hcm", "TP. Hồ Chí Minh"),
            Map.entry("tphcm", "TP. Hồ Chí Minh"),
            Map.entry("da nang", "Đà Nẵng"),
            Map.entry("danang", "Đà Nẵng"),
            Map.entry("hai phong", "Hải Phòng"),
            Map.entry("can tho", "Cần Thơ"),
            Map.entry("nha trang", "Nha Trang"),
            Map.entry("vung tau", "Vũng Tàu"),
            Map.entry("hue", "Huế"),
            Map.entry("bien hoa", "Biên Hòa"),
            Map.entry("thu duc", "Thủ Đức"),
            Map.entry("phu quoc", "Phú Quốc"),
            Map.entry("quy nhon", "Quy Nhơn"),
            Map.entry("ha long", "Hạ Long"),
            Map.entry("halong", "Hạ Long"),
            Map.entry("da lat", "Đà Lạt"),
            Map.entry("dalat", "Đà Lạt"),
            Map.entry("buon ma thuot", "Buôn Ma Thuột"),
            Map.entry("vinh", "Vinh")
    );

    /** Coastal cities — used when query mentions "beach" / "biển" / "ven biển". */
    static final List<String> COASTAL_CITIES = List.of(
            "Đà Nẵng", "Nha Trang", "Phú Quốc", "Vũng Tàu", "Hạ Long", "Quy Nhơn", "Hải Phòng"
    );

    /** Common "near the center" keywords map to no specific city — used to filter
     *  to "districts near downtown" only when paired with a city. */
    static final Set<String> CENTER_KEYWORDS = Set.of(
            "trung tam", "downtown", "center", "central", "city center", "trung tâm"
    );

    /** Beach / coast keywords. */
    static final Set<String> COAST_KEYWORDS = Set.of(
            "beach", "near the beach", "by the beach", "coastal", "seaside", "ocean",
            "bien", "ven bien", "gan bien", "view bien"
    );

    /** Curated district names — only the most-mentioned ones. Matched as case-insensitive
     *  substring after fold (e.g. "quan 7" or "district 7" or "Quận 7"). */
    static final List<String> DISTRICTS = List.of(
            "Quận 1", "Quận 2", "Quận 3", "Quận 4", "Quận 5", "Quận 6",
            "Quận 7", "Quận 8", "Quận 9", "Quận 10", "Quận 11", "Quận 12",
            "Bình Thạnh", "Tân Bình", "Phú Nhuận", "Gò Vấp", "Bình Tân",
            "Cầu Giấy", "Đống Đa", "Ba Đình", "Hoàn Kiếm", "Hai Bà Trưng",
            "Thanh Xuân", "Tây Hồ", "Long Biên", "Nam Từ Liêm", "Bắc Từ Liêm"
    );

    static final Map<String, String> DISTRICT_ALIASES;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        for (String d : DISTRICTS) {
            m.put(fold(d), d);
        }
        // English aliases
        m.put("district 1", "Quận 1");
        m.put("district 2", "Quận 2");
        m.put("district 3", "Quận 3");
        m.put("district 4", "Quận 4");
        m.put("district 5", "Quận 5");
        m.put("district 6", "Quận 6");
        m.put("district 7", "Quận 7");
        m.put("district 8", "Quận 8");
        m.put("district 9", "Quận 9");
        m.put("district 10", "Quận 10");
        m.put("district 11", "Quận 11");
        m.put("district 12", "Quận 12");
        DISTRICT_ALIASES = Map.copyOf(m);
    }

    /** Lowercase + strip Vietnamese diacritics for keyword matching. */
    static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd');
        return n.toLowerCase().trim();
    }
}
